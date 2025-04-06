package org.incept5.authz.core.service.simple

import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.PermissionService
import io.github.oshai.kotlinlogging.KotlinLogging

// logger
private val logger = KotlinLogging.logger {}

/**
 * Simple implementation of the permission service that takes a list of roles
 * in the constuctor
 */
class SimplePermissionService(roles: List<Role>) : PermissionService {

    private val rolePermissionMap: Map<String, List<Permission>> = roles.associateBy(
        { it.name },
        { it.permissions.map(Permission::of) }
    )

    override fun getPermissionsForRole(roleName: String): List<Permission> {
        val result = rolePermissionMap[roleName] ?: emptyList()
        logger.debug { "Permissions for role $roleName: $result" }
        return result
    }
}
