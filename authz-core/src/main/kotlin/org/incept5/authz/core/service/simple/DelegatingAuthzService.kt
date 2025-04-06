package org.incept5.authz.core.service.simple

import org.incept5.authz.core.context.AuthzContext
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.service.PermissionService
import org.incept5.authz.core.service.PrincipalService
import org.incept5.authz.core.service.RoleService
import java.util.*

class DelegatingAuthzService(
    private val principalService: PrincipalService,
    private val roleService: RoleService,
    private val permissionService: PermissionService
) : AuthzContext {
    override fun getPrincipal(): PrincipalContext? {
        return principalService.getPrincipal()
    }

    override fun ensurePrincipal(): PrincipalContext {
        return principalService.ensurePrincipal()
    }

    override fun principalHasGlobalPermission(required: Permission): Boolean {
        return permissionService.principalHasGlobalPermission(required, ensurePrincipal())
    }

    override fun ensurePrincipalHasGlobalPermission(required: Permission) {
        permissionService.ensurePrincipalHasGlobalPermission(required, ensurePrincipal())
    }

    override fun principalHasPermission(required: Permission, type: String, entityId: String): Boolean {
        return permissionService.principalHasPermission(required, type, Collections.singleton(entityId), ensurePrincipal())
    }

    override fun principalHasPermission(required: Permission, type: String, possibleEntityIds: Collection<String>): Boolean {
        return permissionService.principalHasPermission(required, type, possibleEntityIds, ensurePrincipal())
    }

    override fun ensurePrincipalHasPermission(required: Permission, type: String, entityId: String) {
        permissionService.ensurePrincipalHasPermission(required, type, Collections.singleton(entityId), ensurePrincipal())
    }

    override fun ensurePrincipalHasPermission(required: Permission, type: String, possibleEntityIds: Collection<String>) {
        permissionService.ensurePrincipalHasPermission(required, type, possibleEntityIds, ensurePrincipal())
    }

    override fun specificEntityIds(permission: Permission, entityType: String): List<String> {
        return permissionService.specificEntityIds(permission, entityType, ensurePrincipal())
    }

    override fun isOperationAllowedForPrincipal(required: Permission): Boolean {
        return permissionService.isOperationAllowedForPrincipal(required, ensurePrincipal())
    }

    override fun ensureOperationAllowedForPrincipal(required: Permission) {
        permissionService.ensureOperationAllowedForPrincipal(required, ensurePrincipal())
    }

    override fun principalHasEntityRole(type: String): Boolean {
        return ensurePrincipal().getEntityRoles().any { it.type == type }
    }

    override fun ensureRequestedRolesAreAssignable(roleNames: List<String>) {
        val ctx: PrincipalContext = ensurePrincipal()
        if (ctx.getEntityRoles().isNotEmpty()) {
            roleService.ensureRequestedRolesAreAssignable(roleNames, ctx.getEntityRoles().map { it.type })
        } else {
            roleService.ensureRequestedRolesAreAssignable(roleNames, ctx.getGlobalRoles())
        }
    }

    override fun listGlobalPermissionsForPrincipal(): List<Permission> {
        return permissionService.listGlobalPermissionsForUser(ensurePrincipal())
    }

    override fun listEntityPermissionsForPrincipal(): List<Permission> {
        return permissionService.listEntityPermissionsForUser(ensurePrincipal())
    }

}