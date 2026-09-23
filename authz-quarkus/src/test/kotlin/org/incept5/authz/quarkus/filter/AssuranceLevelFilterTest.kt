package org.incept5.authz.quarkus.filter

import jakarta.enterprise.inject.Instance
import org.incept5.authz.core.context.AssuranceLevel
import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.MfaRequiredException
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.RoleService
import org.incept5.authz.core.service.simple.SimpleRoleService
import org.incept5.authz.quarkus.config.MfaConfig
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.SecurityContext
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.security.Principal
import java.util.Optional
import java.util.UUID

/**
 * Unit tests for [AssuranceLevelFilter]. Like [AuthzFilterTest] the harness stubs the principal on
 * the JAX-RS `SecurityContext` — the filter reads the already-verified [PrincipalContext] and never
 * a token. The filter is provider-neutral: it reasons about [AssuranceLevel] and the machine flag,
 * never `aal2` or any Supabase claim.
 */
class AssuranceLevelFilterTest {

    private val backofficeRequired = config(
        roles = listOf("backoffice.admin"),
        skip = listOf("/api/v1/users/profile"),
    )

    // A verified second factor passes untouched.
    @Test
    fun `multi-factor session with a required role passes`() {
        val filter = filter(backofficeRequired)
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.MULTI_FACTOR)))
    }

    // The core case: a single-factor session for a required role is refused.
    @Test
    fun `single-factor session with a required role is refused`() {
        val filter = filter(backofficeRequired)
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
        }
    }

    // A principal whose assurance was never set defaults to SINGLE_FACTOR, so it is refused too
    // (a token with no assurance claim maps to SINGLE_FACTOR in the plugin).
    @Test
    fun `principal with default (unset) assurance is refused`() {
        val filter = filter(backofficeRequired)
        val defaulted = DefaultPrincipalContext("u", UUID.randomUUID(), listOf("backoffice.admin"))
        assertThrows(MfaRequiredException::class.java) { filter.filter(request(defaulted)) }
    }

    // A single-factor user may still reach an MFA-skip path (the enrolment surface).
    @Test
    fun `single-factor session reaches an MFA-skip path`() {
        val filter = filter(backofficeRequired)
        filter.filter(
            request(
                principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR),
                path = "/api/v1/users/profile",
            ),
        )
    }

    // The skip list honours the shared wildcard semantics.
    @Test
    fun `skip path supports segment wildcard`() {
        val filter = filter(config(listOf("backoffice.admin"), listOf("/api/v1/enrol/{segment}")))
        filter.filter(
            request(
                principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR),
                path = "/api/v1/enrol/step1",
            ),
        )
        // ...but not a deeper sub-path.
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(
                request(
                    principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR),
                    path = "/api/v1/enrol/step1/confirm",
                ),
            )
        }
    }

    // A JAX-RS-style placeholder is not a wildcard: it is matched literally and never throws.
    @Test
    fun `skip path with a JAX-RS placeholder is literal, not a regex`() {
        val filter = filter(config(listOf("backoffice.admin"), listOf("/api/v1/users/{userId}/totp/*")))
        val single = principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)
        // the literal text matches
        filter.filter(request(single, path = "/api/v1/users/{userId}/totp/enrol"))
        // a real id does not, and the request is refused rather than 500ing on a bad regex
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(request(single, path = "/api/v1/users/u_1/totp/enrol"))
        }
    }

    // Machine credentials are never subject to MFA, whatever roles they carry.
    @Test
    fun `machine principal is never refused`() {
        val filter = filter(backofficeRequired)
        filter.filter(
            request(
                principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR, machine = true),
            ),
        )
    }

    // A principal without a required role is unaffected.
    @Test
    fun `single-factor session without a required role passes`() {
        val filter = filter(backofficeRequired)
        filter.filter(request(principal(listOf("partner.user"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // A role that `extends-role` a required role inherits its permissions, so it is gated too.
    @Test
    fun `single-factor session with a role extending a required role is refused`() {
        val filter = filter(backofficeRequired, roles = inheritance)
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(request(principal(listOf("backoffice.owner"), AssuranceLevel.SINGLE_FACTOR)))
        }
        // ...transitively.
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(request(principal(listOf("backoffice.superowner"), AssuranceLevel.SINGLE_FACTOR)))
        }
    }

    @Test
    fun `multi-factor session with a role extending a required role passes`() {
        val filter = filter(backofficeRequired, roles = inheritance)
        filter.filter(request(principal(listOf("backoffice.owner"), AssuranceLevel.MULTI_FACTOR)))
    }

    // Inheritance runs upwards only: a required role's *parent* is not gated by its child.
    @Test
    fun `single-factor session with a parent of a required role passes`() {
        val filter = filter(config(listOf("backoffice.owner")), roles = inheritance)
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // With no roles configured the filter changes nothing.
    @Test
    fun `empty required roles disables enforcement`() {
        val filter = filter(config(emptyList()))
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // No verified principal (public/ignored path) is never refused.
    @Test
    fun `no principal passes`() {
        val filter = filter(backofficeRequired)
        filter.filter(request(null))
    }

    // A non-PrincipalContext principal is treated like no principal.
    @Test
    fun `non-PrincipalContext principal passes`() {
        val filter = filter(backofficeRequired)
        filter.filter(request(Principal { "not-a-principal-context" }))
    }

    // An entity role can be a required role too, and is refused at single factor.
    @Test
    fun `single-factor session with a required entity role is refused`() {
        val filter = filter(config(listOf("partner.admin")))
        val p = principal(
            globalRoles = emptyList(),
            assurance = AssuranceLevel.SINGLE_FACTOR,
            entityRoles = listOf(EntityRole("partner", listOf("partner.admin"), listOf("P1"))),
        )
        assertThrows(MfaRequiredException::class.java) { filter.filter(request(p)) }
    }

    // Whitespace-only config entries are ignored, not treated as a role or a skip path.
    @Test
    fun `blank config entries are ignored`() {
        val filter = filter(config(listOf("", "  "), listOf("")))
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // --- helpers ---

    /** backoffice.superowner -> backoffice.owner -> backoffice.admin */
    private val inheritance = listOf(
        Role("backoffice.admin", listOf("example:read")),
        Role("backoffice.owner", listOf("example:admin"), extendsRole = "backoffice.admin"),
        Role("backoffice.superowner", emptyList(), extendsRole = "backoffice.owner"),
    )

    private fun filter(config: MfaConfig, roles: List<Role> = emptyList()): AssuranceLevelFilter {
        @Suppress("UNCHECKED_CAST")
        val roleService = mock(Instance::class.java) as Instance<RoleService>
        `when`(roleService.get()).thenReturn(SimpleRoleService(roles))
        return AssuranceLevelFilter(config, roleService)
    }

    private fun config(roles: List<String>, skip: List<String> = emptyList()) =
        object : MfaConfig {
            override fun requiredRoles(): Optional<List<String>> = Optional.of(roles)
            override fun skipPaths(): Optional<List<String>> = Optional.of(skip)
        }

    private fun principal(
        globalRoles: List<String>,
        assurance: AssuranceLevel,
        machine: Boolean = false,
        entityRoles: List<EntityRole> = emptyList(),
    ): PrincipalContext = DefaultPrincipalContext(
        name = "test-principal",
        principalId = UUID.randomUUID(),
        globalRoles = globalRoles,
        entityRoles = entityRoles,
        assuranceLevel = assurance,
        machinePrincipal = machine,
    )

    private fun request(principal: Principal?, path: String = "/api/v1/users"): ContainerRequestContext {
        val ctx = mock(ContainerRequestContext::class.java)
        val sc = mock(SecurityContext::class.java)
        `when`(sc.userPrincipal).thenReturn(principal)
        `when`(ctx.securityContext).thenReturn(sc)
        val uriInfo = mock(UriInfo::class.java)
        `when`(uriInfo.path).thenReturn(path)
        `when`(ctx.uriInfo).thenReturn(uriInfo)
        return ctx
    }
}
