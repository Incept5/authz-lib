package org.incept5.authz.core.model

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class PermissionTest : ShouldSpec({

    context("some user permissions") {
        should("match required permission") {
            forAll(
                row("user:create", "user:create"),
                row("user:all", "user:create"),
            ) { u, r ->
                Permission.of(u).matches(Permission.of(r)) shouldBe true
            }
        }
    }

    context("some more user permissions") {
        should("not match required permission") {
            forAll(
                row("org:create", "user:create"),
                row("user:read", "user:create"),
            ) { u, r ->
                Permission.of(u).matches(Permission.of(r)) shouldBe false
            }
        }
    }

})
