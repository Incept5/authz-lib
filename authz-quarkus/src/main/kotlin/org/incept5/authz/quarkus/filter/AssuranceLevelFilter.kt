package org.incept5.authz.quarkus.filter

import jakarta.annotation.Priority
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.ext.Provider
import org.incept5.authz.core.context.AssuranceLevel
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.MfaRequiredException
import org.incept5.authz.core.service.RoleService
import org.incept5.authz.quarkus.config.MfaConfig

/**
 * Enforces multi-factor authentication for configured roles, provider-neutrally.
 *
 * Runs at [Priorities.AUTHENTICATION] + 1 — after [AuthzFilter] ([Priorities.AUTHENTICATION]) has
 * exchanged the signature-verified token and installed the [PrincipalContext] on the JAX-RS
 * `SecurityContext`, and before any authorization filter. It only reads the already-verified
 * principal; it never inspects the token or any provider claim. A principal holding a role in
 * [MfaConfig.requiredRoles] — directly, or by holding a role that `extends-role` a required role —
 * whose [PrincipalContext.getAssuranceLevel] is below [AssuranceLevel.MULTI_FACTOR] is refused with
 * [MfaRequiredException] (403, `MFA_REQUIRED`), unless the request path is on the MFA-skip list.
 *
 * As an unbound global provider it sees every request, so each guard clause fails open for requests
 * it does not govern: no configured roles, no verified principal, a machine principal, or a
 * principal with no required role.
 */
@Provider
@Priority(Priorities.AUTHENTICATION + 1)
class AssuranceLevelFilter(
    private val config: MfaConfig,
    private val roleService: Instance<RoleService>,
) : ContainerRequestFilter {

    // Resolved once, lazily on the first request. `@Provider` filters are instantiated at
    // static-init, before the `AuthzConfig` mapping is registered, so nothing here may touch
    // `config` or `roleService` in the constructor. `config` is a client proxy for a normal-scoped
    // bean and `roleService` is an `Instance` handle: both defer resolution until first use, and
    // `by lazy` then caches the parsed values because they are static configuration.

    /** Roles requiring multi-factor. Absent config and blank entries yield an empty set. */
    private val requiredRoles: Set<String> by lazy {
        config.requiredRoles().orElse(emptyList()).filter { it.isNotBlank() }.toSet()
    }

    /** Path patterns a single-factor holder of a required role may still reach, precompiled. */
    private val skipPaths: List<Regex> by lazy {
        PathPatternMatcher.compileAll(config.skipPaths().orElse(emptyList()).filter { it.isNotBlank() })
    }

    /** Resolves `extends-role` inheritance so a sub-role of a required role is governed too. */
    private val roles: RoleService by lazy { roleService.get() }

    override fun filter(requestContext: ContainerRequestContext) {
        // Nothing configured -> the library changes no behaviour.
        if (requiredRoles.isEmpty()) return

        // An ignored/public path never gets a principal (AuthzFilter returns early) -> pass.
        val principal = requestContext.securityContext?.userPrincipal as? PrincipalContext ?: return

        // Machine credentials (API keys, service tokens) are never subject to MFA.
        if (principal.isMachinePrincipal()) return

        // A verified second factor passes untouched.
        if (principal.getAssuranceLevel() == AssuranceLevel.MULTI_FACTOR) return

        // Only principals actually holding a configured role are governed. Roles are expanded
        // through `extends-role` so a role that inherits a required role's permissions is gated too.
        val held = principal.getGlobalRoles() + principal.getEntityRoles().flatMap { it.roles }
        if (roles.expandRoles(held).none { it in requiredRoles }) return

        // A single-factor user may still reach the endpoints needed to enrol a second factor.
        val path = requestContext.uriInfo.path
        if (skipPaths.any { it.matches(path) }) return

        throw MfaRequiredException()
    }
}
