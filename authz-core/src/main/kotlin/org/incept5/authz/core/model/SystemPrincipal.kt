package org.incept5.authz.core.model

import org.incept5.authz.core.context.PrincipalContext
import java.util.UUID

/**
 * We install this principal into the context when we receive incoming event messages
 * When we use this we are saying that the input is from a trusted source and we allow
 * full read and write access to all entities
 *
 * AuthContextHolder.setAuthzContext(SystemPrincipal)
 *
 */
object SystemPrincipal : PrincipalContext {

    // System Role can do everything - this Role needs to be added to the Role Service in addition to the configured roles
    val SYSTEM_ROLE = Role("global.system", listOf(Permission.SYSTEM_ALL_GRANTED_PERMISSION.toString()))

    override fun getPrincipalId(): UUID {
        return UUID.fromString("c7ede195-455a-4384-8ac1-88559bd694af")
    }

    override fun getGlobalRoles(): List<String> {
        return listOf(SYSTEM_ROLE.name)
    }

    override fun getEntityRoles(): List<EntityRole> {
        return emptyList()
    }
}