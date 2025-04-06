package org.incept5.authz.core.access.base

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.incept5.authz.core.access.DefaultAccessControlContext
import org.incept5.authz.core.context.AuthzContext
import org.incept5.authz.core.model.Permission

class BaseEntityAccessControlTest : ShouldSpec({
    
    context("BaseEntityAccessControl") {
        // Create a test request class
        data class TestRequest(val entityId: String)
        
        // Create a mock AuthzContext
        val mockAuthzContext = mockk<AuthzContext>(relaxed = true)
        
        // Create a test implementation of BaseEntityAccessControl
        val permission = Permission.of("entity:read")
        val entityType = "test-entity"
        val testRequest = TestRequest("entity123")
        
        val accessControl = object : BaseEntityAccessControl(
            permission = permission,
            entityType = entityType,
            extractEntityId = { ctx -> ctx.firstOfType(TestRequest::class.java).entityId }
        ) {}
        
        should("call ensurePrincipalHasPermission with correct parameters") {
            // Create context with our test request
            val ctx = DefaultAccessControlContext(arrayOf(testRequest), mockAuthzContext)
            
            // Execute the before method
            accessControl.before(ctx)
            
            // Verify that the correct method was called on the AuthzContext
            verify { 
                mockAuthzContext.ensurePrincipalHasPermission(
                    permission, 
                    entityType, 
                    "entity123"
                ) 
            }
        }
        
        should("extract entity ID correctly from context") {
            // Create context with our test request
            val ctx = DefaultAccessControlContext(arrayOf(testRequest), mockAuthzContext)
            
            // Test the extractEntityId function directly
            val extractedId = accessControl.extractEntityId(ctx)
            extractedId shouldBe "entity123"
        }
        
        should("handle multiple arguments in context") {
            // Create a context with multiple arguments
            val ctx = DefaultAccessControlContext(
                arrayOf("some string", 123, testRequest), 
                mockAuthzContext
            )
            
            // Execute the before method
            accessControl.before(ctx)
            
            // Verify that the correct method was called on the AuthzContext
            verify { 
                mockAuthzContext.ensurePrincipalHasPermission(
                    permission, 
                    entityType, 
                    "entity123"
                ) 
            }
        }
    }
})