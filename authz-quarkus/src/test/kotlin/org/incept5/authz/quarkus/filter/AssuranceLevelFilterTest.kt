package org.incept5.authz.quarkus.filter

import org.incept5.authz.core.context.AssuranceLevel
import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.MfaRequiredException
import org.incept5.authz.core.model.EntityRole
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
        val filter = AssuranceLevelFilter(backofficeRequired)
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.MULTI_FACTOR)))
    }

    // The H5 exploit: a single-factor session for a required role is refused.
    @Test
    fun `single-factor session with a required role is refused`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        assertThrows(MfaRequiredException::class.java) {
            filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
        }
    }

    // A principal whose assurance was never set defaults to SINGLE_FACTOR, so it is refused too
    // (a token with no assurance claim maps to SINGLE_FACTOR in the plugin — story AC6).
    @Test
    fun `principal with default (unset) assurance is refused`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        val defaulted = DefaultPrincipalContext("u", UUID.randomUUID(), listOf("backoffice.admin"))
        assertThrows(MfaRequiredException::class.java) { filter.filter(request(defaulted)) }
    }

    // A single-factor user may still reach an MFA-skip path (the enrolment surface).
    @Test
    fun `single-factor session reaches an MFA-skip path`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
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
        val filter = AssuranceLevelFilter(config(listOf("backoffice.admin"), listOf("/api/v1/enrol/{segment}")))
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

    // Machine credentials are never subject to MFA, whatever roles they carry.
    @Test
    fun `machine principal is never refused`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        filter.filter(
            request(
                principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR, machine = true),
            ),
        )
    }

    // A principal without a required role is unaffected.
    @Test
    fun `single-factor session without a required role passes`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        filter.filter(request(principal(listOf("partner.user"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // With no roles configured the filter changes nothing.
    @Test
    fun `empty required roles disables enforcement`() {
        val filter = AssuranceLevelFilter(config(emptyList()))
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // No verified principal (public/ignored path) is never refused.
    @Test
    fun `no principal passes`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        filter.filter(request(null))
    }

    // A non-PrincipalContext principal is treated like no principal.
    @Test
    fun `non-PrincipalContext principal passes`() {
        val filter = AssuranceLevelFilter(backofficeRequired)
        filter.filter(request(Principal { "not-a-principal-context" }))
    }

    // An entity role can be a required role too, and is refused at single factor.
    @Test
    fun `single-factor session with a required entity role is refused`() {
        val filter = AssuranceLevelFilter(config(listOf("partner.admin")))
        val p = principal(
            globalRoles = emptyList(),
            assurance = AssuranceLevel.SINGLE_FACTOR,
            entityRoles = listOf(EntityRole("partner", listOf("partner.admin"), listOf("P1"))),
        )
        assertThrows(MfaRequiredException::class.java) { filter.filter(request(p)) }
    }

    // Blank config entries (from an unset @WithDefault("")) are ignored, not treated as a role.
    @Test
    fun `blank config entries are ignored`() {
        val filter = AssuranceLevelFilter(config(listOf("", "  "), listOf("")))
        filter.filter(request(principal(listOf("backoffice.admin"), AssuranceLevel.SINGLE_FACTOR)))
    }

    // --- helpers ---

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
    ): PrincipalContext = object : PrincipalContext {
        override fun getName(): String = "test-principal"
        override fun getPrincipalId(): UUID = UUID.randomUUID()
        override fun getGlobalRoles(): List<String> = globalRoles
        override fun getEntityRoles(): List<EntityRole> = entityRoles
        override fun getAssuranceLevel(): AssuranceLevel = assurance
        override fun isMachinePrincipal(): Boolean = machine
    }

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
