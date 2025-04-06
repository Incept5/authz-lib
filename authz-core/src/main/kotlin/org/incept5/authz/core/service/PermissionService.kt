package org.incept5.authz.core.service

import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.AuthzErrorCodes
import org.incept5.authz.core.exp.AuthzException
import org.incept5.authz.core.exp.ForbiddenException
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.model.Permission
import java.util.stream.Collectors

/**
 * Permission service to get permissions and ensure requested permissions are granted
 */
interface PermissionService {

    fun getPermissionsForRole(roleName: String): List<Permission>

    fun getPermissionsForRoles(roleNames: Collection<String>): List<Permission> =
        roleNames.flatMap { getPermissionsForRole(it) }

    fun findMatchingGlobalPermission(required: Permission, context: PrincipalContext): Permission? {
        val globalGranted = getPermissionsForRoles(context.getGlobalRoles())
        return globalGranted.firstOrNull { it.matches(required) }
    }

    fun principalHasGlobalPermission(required: Permission, context: PrincipalContext): Boolean =
        findMatchingGlobalPermission(required, context) != null

    fun ensurePrincipalHasGlobalPermission(required: Permission, context: PrincipalContext) {
        if (!principalHasGlobalPermission(required, context)) {
            // Replace with your own exception
            throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Global permission not granted - required: $required granted: " + listGlobalPermissionsForUser(context))
        }
    }

    fun findMatchingPermission(
        required: Permission,
        type: String,
        possibleEntityIds: Collection<String>,
        context: PrincipalContext
    ): Permission? {
        val global = findMatchingGlobalPermission(required, context)
        if (global != null) {
            return global
        }
        val entityRole = context.getEntityRoles().find { it.matches(type, possibleEntityIds) } ?: return null
        val entityPermissions = getPermissionsForRoles(entityRole.roles)
        return entityPermissions.find { it.matches(required) }
    }

    fun principalHasPermission(
        required: Permission,
        type: String,
        possibleEntityIds: Collection<String>,
        context: PrincipalContext
    ): Boolean =
        findMatchingPermission(required, type, possibleEntityIds, context) != null

    fun ensurePrincipalHasPermission(
        required: Permission,
        type: String,
        possibleEntityIds: Collection<String>,
        context: PrincipalContext
    ) {
        if (findMatchingPermission(required, type, possibleEntityIds, context) == null) {
            // Replace with your own exception
            throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Entity permission not granted. required: $required granted: " + listGlobalPermissionsForUser(context))
        }
    }

    fun specificEntityIds(required: Permission, entityType: String, context: PrincipalContext): List<String> {
        return context.getEntityRoles()
            .filter { er -> entityType == er.type } // filter out entity roles for other entity types
            .filter { er -> entityRoleContainsPermission(er, required) } // filter out entity roles that don't reference the required permission
            .flatMap { er -> er.ids } // collect the ids from the remaining entity roles
            .toList()
    }

    private fun entityRoleContainsPermission(entityRole: EntityRole, required: Permission): Boolean {
        return getPermissionsForRoles(entityRole.roles).stream().anyMatch { permission: Permission -> permission.matches(required) }
    }

    fun isOperationAllowedForPrincipal(required: Permission, context: PrincipalContext): Boolean {
        return if (principalHasGlobalPermission(required, context)) {
            // empty optional means Global Access IS allowed
            true
        } else {
            context.getEntityRoles().stream()
                .anyMatch { er -> entityRoleContainsPermission(er, required) }
        }
        // look for any permission that matches the one we need
    }

    fun ensureOperationAllowedForPrincipal(required: Permission, context: PrincipalContext) {
        if (!isOperationAllowedForPrincipal(required, context)) {
            throw ForbiddenException("Operation not allowed for principal")
        }
    }

    fun listGlobalPermissionsForUser(context: PrincipalContext): List<Permission> {
        return getPermissionsForRoles(context.getGlobalRoles())
    }

    fun listEntityPermissionsForUser(context: PrincipalContext): List<Permission> {
        return context.getEntityRoles().stream()
            .flatMap { er -> getPermissionsForRoles(er.roles).stream() }
            .collect(Collectors.toList())
    }

}
