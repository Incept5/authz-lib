package org.incept5.authz.example

import org.incept5.authz.core.access.base.BaseEntityAccessControl
import org.incept5.authz.core.annotation.AuthzCheck
import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.RolesProvider
import org.incept5.authz.quarkus.interceptor.Authorized
import jakarta.inject.Singleton

/**
 * Example service that is secured by authz
 * Note that both the class level @Authorized and the method
 * level @AuthzCheck are required for the method to be secured
 */
@Singleton
@Authorized
class ExampleSecureService {
    @AuthzCheck(ExampleAccessControl::class)
    fun authorizedMethod(id: String): String {
        return "Authorized method called with id: $id"
    }
}
class ExampleAccessControl : BaseEntityAccessControl(
         permission = Permission.of("example:read"),
         entityType = "org",
         extractEntityId = { ctx -> ctx.firstArg() }
)