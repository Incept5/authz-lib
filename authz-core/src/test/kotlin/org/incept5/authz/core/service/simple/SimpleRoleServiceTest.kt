package org.incept5.authz.core.service.simple
// SimpleRoleServiceTest.kt

import org.incept5.authz.core.model.Role
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe

class SimpleRoleServiceTest : ShouldSpec({
    val role1 = Role("admin", listOf("read", "write"), null, listOf("user"))
    val role2 = Role("user", listOf("read"))

    val roleService = SimpleRoleService(listOf(role1, role2))

    should("return role by its name") {
        val role = roleService.getRole("admin")
        role?.name shouldBe "admin"
    }

    should("return assignable roles") {
        val assignableRoles = roleService.getAssignableRoles("admin")
        assignableRoles shouldContainExactly listOf("user")
    }

    should("return empty list for non-existent role") {
        val assignableRoles = roleService.getAssignableRoles("non-existent")
        assignableRoles.shouldBeEmpty()
    }

    should("ensure roles are assignable") {
        roleService.ensureRequestedRolesAreAssignable(listOf("user"), listOf("admin"))
    }

    should("throw exception for non-assignable roles") {
        shouldThrow<Exception> {
            roleService.ensureRequestedRolesAreAssignable(listOf("admin"), listOf("user"))
        }
    }
})
