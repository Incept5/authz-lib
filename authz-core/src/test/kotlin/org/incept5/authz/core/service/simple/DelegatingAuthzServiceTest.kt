package org.incept5.authz.core.service.simple

import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.service.PermissionService
import org.incept5.authz.core.service.PrincipalService
import org.incept5.authz.core.service.RoleService
import java.util.UUID

class DelegatingAuthzServiceTest : ShouldSpec({
    
    context("DelegatingAuthzService") {
        // Create mock services
        val mockPrincipalService = mockk<PrincipalService>()
        val mockRoleService = mockk<RoleService>()
        val mockPermissionService = mockk<PermissionService>()
        
        // Create a mock principal context
        val principalId = UUID.randomUUID()
        val globalRoles = listOf("admin", "user")
        val entityRoles = listOf(
            EntityRole("organization", listOf("org123"), listOf("admin")),
            EntityRole("project", listOf("proj456"), listOf("viewer"))
        )
        val mockPrincipalContext = mockk<PrincipalContext>()
        
        // Set up the mock principal context
        every { mockPrincipalContext.getPrincipalId() } returns principalId
        every { mockPrincipalContext.getGlobalRoles() } returns globalRoles
        every { mockPrincipalContext.getEntityRoles() } returns entityRoles
        
        // Set up the mock principal service
        every { mockPrincipalService.getPrincipal() } returns mockPrincipalContext
        every { mockPrincipalService.ensurePrincipal() } returns mockPrincipalContext
        
        // Create the service under test
        val authzService = DelegatingAuthzService(
            mockPrincipalService,
            mockRoleService,
            mockPermissionService
        )
        
        should("delegate getPrincipal to PrincipalService") {
            val result = authzService.getPrincipal()
            result shouldBe mockPrincipalContext
            verify { mockPrincipalService.getPrincipal() }
        }
        
        should("delegate ensurePrincipal to PrincipalService") {
            val result = authzService.ensurePrincipal()
            result shouldBe mockPrincipalContext
            verify { mockPrincipalService.ensurePrincipal() }
        }
        
        should("delegate principalHasGlobalPermission to PermissionService") {
            val permission = Permission.of("test:read")
            every { 
                mockPermissionService.principalHasGlobalPermission(permission, mockPrincipalContext) 
            } returns true
            
            val result = authzService.principalHasGlobalPermission(permission)
            result shouldBe true
            
            verify { 
                mockPermissionService.principalHasGlobalPermission(permission, mockPrincipalContext) 
            }
        }
        
        should("delegate ensurePrincipalHasGlobalPermission to PermissionService") {
            val permission = Permission.of("test:update")
            every { 
                mockPermissionService.ensurePrincipalHasGlobalPermission(permission, mockPrincipalContext) 
            } returns Unit
            
            authzService.ensurePrincipalHasGlobalPermission(permission)
            
            verify { 
                mockPermissionService.ensurePrincipalHasGlobalPermission(permission, mockPrincipalContext) 
            }
        }
        
        should("delegate principalHasPermission (single entity) to PermissionService") {
            val permission = Permission.of("test:read")
            val entityType = "project"
            val entityId = "proj123"
            
            every { 
                mockPermissionService.principalHasPermission(
                    permission, 
                    entityType, 
                    any<Collection<String>>(), 
                    mockPrincipalContext
                ) 
            } returns true
            
            val result = authzService.principalHasPermission(permission, entityType, entityId)
            result shouldBe true
            
            verify { 
                mockPermissionService.principalHasPermission(
                    permission, 
                    entityType, 
                    match { it.contains(entityId) }, 
                    mockPrincipalContext
                ) 
            }
        }
        
        should("delegate principalHasEntityRole correctly") {
            val result = authzService.principalHasEntityRole("organization")
            result shouldBe true
        }
        
        should("delegate listGlobalPermissionsForPrincipal to PermissionService") {
            val permissions = listOf(
                Permission.of("test:read"),
                Permission.of("test:update")
            )
            
            every { 
                mockPermissionService.listGlobalPermissionsForUser(mockPrincipalContext) 
            } returns permissions
            
            val result = authzService.listGlobalPermissionsForPrincipal()
            result shouldContainExactly permissions
            
            verify { 
                mockPermissionService.listGlobalPermissionsForUser(mockPrincipalContext) 
            }
        }
    }
})