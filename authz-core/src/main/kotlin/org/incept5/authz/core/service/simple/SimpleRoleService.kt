package org.incept5.authz.core.service.simple

import org.incept5.authz.core.exp.ForbiddenException
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.RoleService

/**
 * Simple implementation of RoleService that takes a list of roles in the constructor
 */
class SimpleRoleService(private val initialRoles: List<Role>) : RoleService {

    private var roles: Map<String, Role> = initialRoles.associateBy { it.name }

    override fun getRole(roleName: String): Role? {
        return roles[roleName]
    }

    override fun getAssignableRoles(roleName: String): List<String> {
        return roles[roleName]?.assignableRoles ?: emptyList()
    }

    override fun getAssignableRoles(roleNames: Collection<String>): List<String> {
        return roleNames.flatMap { getAssignableRoles(it) }
    }

    override fun ensureRequestedRolesAreAssignable(requestedRoles: List<String>?, currentUserRoles: List<String>) {
        requestedRoles?.takeIf { it.isNotEmpty() }?.let {
            val assignableRoles = getAssignableRoles(currentUserRoles)
            val notAllowed = it.firstOrNull { r -> !assignableRoles.contains(r) }
            notAllowed?.let { role ->
                throw ForbiddenException("Role $role is not assignable.")
            }
        }
    }
}
