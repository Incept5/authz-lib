package org.incept5.authz.core.context

import org.incept5.authz.core.model.EntityRole
import java.util.UUID

data class DefaultPrincipalContext(private val principalId: UUID, private val globalRoles: List<String>, private val entityRoles: List<EntityRole> = emptyList()) :
    PrincipalContext {
    override fun getPrincipalId(): UUID {
        return principalId
    }

    override fun getGlobalRoles(): List<String> {
        return globalRoles
    }

    override fun getEntityRoles(): List<EntityRole> {
        return entityRoles
    }
}
