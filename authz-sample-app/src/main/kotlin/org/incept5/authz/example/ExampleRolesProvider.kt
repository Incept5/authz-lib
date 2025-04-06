package org.incept5.authz.example

import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.RolesProvider
import jakarta.inject.Singleton

@Singleton
class ExampleRolesProvider: RolesProvider {

    override fun provideRoles() = listOf(
        Role(name = "extra.admin", permissions = listOf(
            "example:read",
            "example:create",
        )),
    )
}