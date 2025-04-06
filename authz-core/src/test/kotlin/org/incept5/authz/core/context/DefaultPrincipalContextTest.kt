package org.incept5.authz.core.context

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.incept5.authz.core.model.EntityRole
import java.util.UUID

class DefaultPrincipalContextTest : ShouldSpec({
    
    context("DefaultPrincipalContext") {
        val principalId = UUID.randomUUID()
        val globalRoles = listOf("admin", "user")
        val entityRoles = listOf(
            EntityRole("organization", listOf("org123"), listOf("admin")),
            EntityRole("project", listOf("proj456"), listOf("viewer"))
        )
        
        should("return correct principal ID") {
            val context = DefaultPrincipalContext(principalId, globalRoles, entityRoles)
            context.getPrincipalId() shouldBe principalId
        }
        
        should("return correct global roles") {
            val context = DefaultPrincipalContext(principalId, globalRoles, entityRoles)
            context.getGlobalRoles() shouldContainExactly globalRoles
        }
        
        should("return correct entity roles") {
            val context = DefaultPrincipalContext(principalId, globalRoles, entityRoles)
            context.getEntityRoles() shouldContainExactly entityRoles
        }
        
        should("handle empty entity roles") {
            val context = DefaultPrincipalContext(principalId, globalRoles)
            context.getEntityRoles().shouldBeEmpty()
        }
        
        should("handle empty global roles") {
            val context = DefaultPrincipalContext(principalId, emptyList(), entityRoles)
            context.getGlobalRoles().shouldBeEmpty()
        }
    }
})