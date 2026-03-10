package org.incept5.authz.core.service.simple

// Filename: SimplePermissionServiceTest.kt

import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.model.Role
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

class SimplePermissionServiceTest : ShouldSpec({
    val mockPrincipalContext = mockk<PrincipalContext>()
    val readPermission = Permission.of("resource:read")
    val writePermission = Permission.of("resource:create")
    val roles = listOf(
        Role("readRole", listOf("resource:read")),
        Role("writeRole", listOf("resource:create"))
    )

    val simplePermissionService = SimplePermissionService(roles)

    should("get permissions for a single role") {
        val permissions = simplePermissionService.getPermissionsForRole("readRole")
        permissions shouldContainExactly listOf(readPermission)
    }

    should("get permissions for multiple roles") {
        val permissions = simplePermissionService.getPermissionsForRoles(listOf("readRole", "writeRole"))
        permissions shouldContainExactly listOf(readPermission, writePermission)
    }

    should("find matching global permission") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        val permission = simplePermissionService.findMatchingGlobalPermission(readPermission, mockPrincipalContext)
        permission shouldBe readPermission
    }

    should("check principal has global permission") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        val result = simplePermissionService.principalHasGlobalPermission(readPermission, mockPrincipalContext)
        result shouldBe true
    }

    should("ensure principal has global permission") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        simplePermissionService.ensurePrincipalHasGlobalPermission(readPermission, mockPrincipalContext) // should not throw
    }

    should("find matching entity permission") {
        every { mockPrincipalContext.getEntityRoles() } returns listOf(EntityRole("org", listOf("readRole"), listOf("1")))
        val permission = simplePermissionService.findMatchingPermission(readPermission, "org", listOf("1"), mockPrincipalContext)
        permission shouldBe readPermission
    }

    should("check principal has entity permission") {
        every { mockPrincipalContext.getEntityRoles() } returns listOf(EntityRole("org", listOf("readRole"), listOf("1")))
        val result = simplePermissionService.principalHasPermission(readPermission, "org", listOf("1"), mockPrincipalContext)
        result shouldBe true
    }

    should("ensure principal has entity permission") {
        every { mockPrincipalContext.getEntityRoles() } returns listOf(EntityRole("org", listOf("readRole"), listOf("1")))
        simplePermissionService.ensurePrincipalHasPermission(readPermission, "org", listOf("1"), mockPrincipalContext) // should not throw
    }

    should("list specific entity IDs for a required permission") {
        every { mockPrincipalContext.getEntityRoles() } returns listOf(EntityRole("org", listOf("readRole"), listOf("1", "2")))
        val ids = simplePermissionService.specificEntityIds(readPermission, "org", mockPrincipalContext)
        ids shouldContainExactly setOf("1", "2")
    }

    should("check if operation is allowed for principal") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        val result = simplePermissionService.isOperationAllowedForPrincipal(readPermission, mockPrincipalContext)
        result shouldBe true
    }

    should("ensure operation is allowed for principal") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        simplePermissionService.ensureOperationAllowedForPrincipal(readPermission, mockPrincipalContext) // should not throw
    }

    should("list global permissions for user") {
        every { mockPrincipalContext.getGlobalRoles() } returns listOf("readRole")
        val permissions = simplePermissionService.listGlobalPermissionsForUser(mockPrincipalContext)
        permissions shouldContainExactly listOf(readPermission)
    }

    should("list entity permissions for user") {
        every { mockPrincipalContext.getEntityRoles() } returns listOf(EntityRole("org", listOf("readRole"), listOf("1")))
        val permissions = simplePermissionService.listEntityPermissionsForUser(mockPrincipalContext)
        permissions shouldContainExactly listOf(readPermission)
    }

    should("inherit permissions from extended role") {
        val inheritRoles = listOf(
            Role("user", listOf("resource:read")),
            Role("admin", listOf("resource:create"), extendsRole = "user")
        )
        val service = SimplePermissionService(inheritRoles)
        val permissions = service.getPermissionsForRole("admin")
        permissions shouldContainExactly listOf(Permission.of("resource:create"), Permission.of("resource:read"))
    }

    should("handle multi-level inheritance") {
        val inheritRoles = listOf(
            Role("user", listOf("resource:read")),
            Role("admin", listOf("resource:create"), extendsRole = "user"),
            Role("super_admin", listOf("resource:delete"), extendsRole = "admin")
        )
        val service = SimplePermissionService(inheritRoles)
        val permissions = service.getPermissionsForRole("super_admin")
        permissions shouldContainExactly listOf(
            Permission.of("resource:delete"),
            Permission.of("resource:create"),
            Permission.of("resource:read")
        )
    }

    should("handle circular extendsRole gracefully") {
        val circularRoles = listOf(
            Role("role_a", listOf("resource:read"), extendsRole = "role_b"),
            Role("role_b", listOf("resource:create"), extendsRole = "role_a")
        )
        val service = SimplePermissionService(circularRoles)
        // Should not throw or infinite loop
        val permissionsA = service.getPermissionsForRole("role_a")
        val permissionsB = service.getPermissionsForRole("role_b")
        permissionsA shouldContainExactly listOf(Permission.of("resource:read"), Permission.of("resource:create"))
        permissionsB shouldContainExactly listOf(Permission.of("resource:create"), Permission.of("resource:read"))
    }

    should("handle missing parent role") {
        val rolesWithMissingParent = listOf(
            Role("admin", listOf("resource:create"), extendsRole = "nonexistent")
        )
        val service = SimplePermissionService(rolesWithMissingParent)
        val permissions = service.getPermissionsForRole("admin")
        permissions shouldContainExactly listOf(Permission.of("resource:create"))
    }
})
