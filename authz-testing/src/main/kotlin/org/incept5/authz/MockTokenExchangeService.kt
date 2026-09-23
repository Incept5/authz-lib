package org.incept5.authz

import org.incept5.authz.core.context.AssuranceLevel
import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.service.TokenExchangePlugin
import jakarta.inject.Singleton
import java.util.UUID

/**
 * Fixed bearer tokens for tests. The admin token represents a fully authenticated (multi-factor)
 * session; the `-1fa-` tokens represent single-factor sessions and the service-account token a
 * machine principal, so consumers can exercise MFA enforcement end-to-end.
 */
@Singleton
class MockTokenExchangeService : TokenExchangePlugin {

    companion object {
        const val ORG_ENTITY_ID = "org-1"
    }

    override fun exchangeToken(token: String): PrincipalContext? {

        if (token == "backoffice-admin-token") {
            return DefaultPrincipalContext(
                name = "backoffice user",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                globalRoles = listOf("backoffice.admin"),
                entityRoles = listOf(),
                assuranceLevel = AssuranceLevel.MULTI_FACTOR,
            )
        }

        if (token == "no-roles-token") {
            return DefaultPrincipalContext(
                name = "not roles user",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                globalRoles = emptyList(),
                entityRoles = emptyList()
            )
        }

        if (token == "org-user-token") {
            return DefaultPrincipalContext(
                name = "org user",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000003"),
                globalRoles = emptyList(),
                entityRoles = listOf(
                    EntityRole(
                        type = "org",
                        roles = listOf("org.user"),
                        ids = listOf(ORG_ENTITY_ID)
                    )
                )
            )
        }

        // A backoffice admin who has not yet presented a second factor.
        if (token == "backoffice-admin-1fa-token") {
            return DefaultPrincipalContext(
                name = "backoffice user (single factor)",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000004"),
                globalRoles = listOf("backoffice.admin"),
                assuranceLevel = AssuranceLevel.SINGLE_FACTOR,
            )
        }

        // Holds a role that `extends-role` backoffice.admin (see the sample app config), single factor.
        if (token == "backoffice-owner-1fa-token") {
            return DefaultPrincipalContext(
                name = "backoffice owner (single factor)",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000005"),
                globalRoles = listOf("backoffice.owner"),
                assuranceLevel = AssuranceLevel.SINGLE_FACTOR,
            )
        }

        // A machine credential carrying a privileged role; never subject to MFA.
        if (token == "service-account-token") {
            return DefaultPrincipalContext(
                name = "service account",
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000006"),
                globalRoles = listOf("backoffice.admin"),
                machinePrincipal = true,
            )
        }
        return null
    }
}
