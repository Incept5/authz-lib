package org.incept5.authz

import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.service.TokenExchangePlugin
import jakarta.inject.Singleton
import java.util.UUID

@Singleton
class MockTokenExchangeService : TokenExchangePlugin {

    companion object {
        const val ORG_ENTITY_ID = "org-1"
    }

    override fun exchangeToken(token: String): PrincipalContext? {

        if (token == "backoffice-admin-token") {
            return DefaultPrincipalContext(
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000001"),
                globalRoles = listOf("backoffice.admin"),
                entityRoles = listOf()
            )
        }

        if (token == "no-roles-token") {
            return DefaultPrincipalContext(
                principalId = UUID.fromString("00000000-0000-0000-0000-000000000002"),
                globalRoles = emptyList(),
                entityRoles = emptyList()
            )
        }

        if (token == "org-user-token") {
            return DefaultPrincipalContext(
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
        return null
    }
}