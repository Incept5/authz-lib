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

    /**
     * The given role names plus every role they inherit from through [Role.extendsRole],
     * transitively. Names with no known [Role] are kept as-is (they simply have no ancestors), and
     * inheritance cycles are tolerated. This is the role-level counterpart of the permission
     * resolution done by `SimplePermissionService`: a principal holding a sub-role is treated as
     * also holding each role it extends.
     */
    fun expandRoles(roleNames: Collection<String>): Set<String> {
        val expanded = LinkedHashSet<String>()
        val pending = ArrayDeque(roleNames)
        while (pending.isNotEmpty()) {
            val name = pending.removeFirst()
            if (expanded.add(name)) {
                getRole(name)?.extendsRole?.let { pending.addLast(it) }
            }
        }
        return expanded
    }
}
