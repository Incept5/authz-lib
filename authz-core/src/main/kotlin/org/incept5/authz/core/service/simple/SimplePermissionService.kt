package org.incept5.authz.core.service.simple

import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.PermissionService
import io.github.oshai.kotlinlogging.KotlinLogging

// logger
private val logger = KotlinLogging.logger {}

/**
 * Simple implementation of the permission service that takes a list of roles
 * in the constructor
 */
class SimplePermissionService(roles: List<Role>) : PermissionService {

    private val rolePermissionMap: Map<String, List<Permission>> = run {
        val rolesByName = roles.associateBy { it.name }

        fun resolvePermissions(role: Role, visited: Set<String> = emptySet()): List<Permission> {
            if (role.name in visited) return emptyList() // cycle guard
            val own = role.permissions.map(Permission::of)
            val parent = role.extendsRole?.let { rolesByName[it] }
            return if (parent != null) {
                own + resolvePermissions(parent, visited + role.name)
            } else {
                own
            }
        }

        roles.associate { it.name to resolvePermissions(it) }
    }

    override fun getPermissionsForRole(roleName: String): List<Permission> {
        val result = rolePermissionMap[roleName] ?: emptyList()
        logger.debug { "Permissions for role $roleName: $result" }
        return result
    }
}