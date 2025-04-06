package org.incept5.authz.core.access.base

import org.incept5.authz.core.access.AccessControl
import org.incept5.authz.core.access.DefaultAccessControlContext
import org.incept5.authz.core.model.Permission

/**
 * A common thing to do is to check if the current principal has a permission to access a specific entity
 * This is a helper to make that normal use case easier/less code and looks like this:
 *
 * Example:
 * class CreateUserAccessControl : BaseEntityAccessControl(
 *     permission = Permission.of("users:create"),
 *     entityType = "org",
 *     extractEntityId = { ctx -> ctx.firstOfType(CreateUserRequest::class.java).orgId }
 * )
 */
abstract class BaseEntityAccessControl(
    val permission: Permission,
    val entityType: String,
    val extractEntityId: (org.incept5.authz.core.access.DefaultAccessControlContext) -> String
) : org.incept5.authz.core.access.AccessControl<Any> {

    override fun before(ctx: org.incept5.authz.core.access.DefaultAccessControlContext) {
        val entityId = extractEntityId(ctx)
        ctx.authz().ensurePrincipalHasPermission(permission, entityType, entityId)
    }
}