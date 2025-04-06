package org.incept5.authz.core.service

import org.incept5.authz.core.exp.ForbiddenException
import org.incept5.authz.core.model.Role

/**
 * Role service to get roles and assignable roles and ensure requested roles are assignable
 */
interface RoleService {

    fun getRole(roleName: String): Role?

    fun getAssignableRoles(roleName: String): List<String>

    fun getAssignableRoles(roleNames: Collection<String>): List<String> {
        return roleNames.flatMap { getAssignableRoles(it) }
    }

    fun ensureRequestedRolesAreAssignable(requestedRoles: List<String>?, currentUserRoles: List<String>) {
        requestedRoles?.takeIf { it.isNotEmpty() }?.let {
            val assignableRoles = getAssignableRoles(currentUserRoles)
            val notAllowed = it.firstOrNull { r -> !assignableRoles.contains(r) }
            notAllowed?.let { role ->
                throw ForbiddenException("Role $role is not assignable.")
            }
        }
    }
}