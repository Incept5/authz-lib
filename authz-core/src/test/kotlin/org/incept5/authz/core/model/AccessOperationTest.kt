package org.incept5.authz.core.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe

class AccessOperationTest : ShouldSpec({
    
    context("AccessOperation") {
        should("parse string values correctly") {
            forAll(
                row("CREATE", AccessOperation.CREATE),
                row("READ", AccessOperation.READ),
                row("UPDATE", AccessOperation.UPDATE),
                row("DELETE", AccessOperation.DELETE),
                row("ALL", AccessOperation.ALL),
                row("create", AccessOperation.CREATE),
                row("read", AccessOperation.READ),
                row("update", AccessOperation.UPDATE),
                row("delete", AccessOperation.DELETE),
                row("all", AccessOperation.ALL)
            ) { input, expected ->
                AccessOperation.parse(input) shouldBe expected
            }
        }
        
        should("throw IllegalArgumentException for invalid values") {
            shouldThrow<IllegalArgumentException> {
                AccessOperation.parse("INVALID")
            }
        }
        
        should("match operations correctly") {
            // ALL matches everything
            AccessOperation.ALL.matches(AccessOperation.CREATE) shouldBe true
            AccessOperation.ALL.matches(AccessOperation.READ) shouldBe true
            AccessOperation.ALL.matches(AccessOperation.UPDATE) shouldBe true
            AccessOperation.ALL.matches(AccessOperation.DELETE) shouldBe true
            AccessOperation.ALL.matches(AccessOperation.ALL) shouldBe true
            
            // Specific operations only match themselves
            AccessOperation.CREATE.matches(AccessOperation.CREATE) shouldBe true
            AccessOperation.CREATE.matches(AccessOperation.READ) shouldBe false
            
            AccessOperation.READ.matches(AccessOperation.READ) shouldBe true
            AccessOperation.READ.matches(AccessOperation.UPDATE) shouldBe false
            
            AccessOperation.UPDATE.matches(AccessOperation.UPDATE) shouldBe true
            AccessOperation.UPDATE.matches(AccessOperation.DELETE) shouldBe false
            
            AccessOperation.DELETE.matches(AccessOperation.DELETE) shouldBe true
            AccessOperation.DELETE.matches(AccessOperation.CREATE) shouldBe false
        }
    }
})