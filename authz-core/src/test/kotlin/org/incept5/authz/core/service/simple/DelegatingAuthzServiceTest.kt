package org.incept5.authz.core.service.simple

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.ForbiddenException
import org.incept5.authz.core.model.EntityRole
import org.incept5.authz.core.model.Permission
import org.incept5.authz.core.model.Role
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

    context("role assignment with real RoleService") {

        val roles = listOf(
            Role("backoffice.admin", listOf(".*:all"), null, listOf("partner.admin", "merchant.admin")),
            Role("partner.admin", listOf("partner:update", "user:create"), "partner.user", listOf("partner.admin", "partner.user", "merchant.admin")),
            Role("partner.user", listOf("partner:read")),
            Role("merchant.admin", listOf("merchant:update", "user:create"), "merchant.user", listOf("merchant.admin", "merchant.user")),
            Role("merchant.user", listOf("merchant:read"))
        )
        val roleService = SimpleRoleService(roles)
        val mockPermissionService = mockk<PermissionService>()

        should("global principal can assign roles based on assignable-roles config") {
            val principalService = mockk<PrincipalService>()
            val principal = mockk<PrincipalContext>()
            every { principal.getGlobalRoles() } returns listOf("backoffice.admin")
            every { principal.getEntityRoles() } returns emptyList()
            every { principalService.getPrincipal() } returns principal
            every { principalService.ensurePrincipal() } returns principal

            val service = DelegatingAuthzService(principalService, roleService, mockPermissionService)

            service.principalCanAssignRole("partner.admin") shouldBe true
            service.principalCanAssignRole("merchant.admin") shouldBe true
            service.principalCanAssignRole("backoffice.admin") shouldBe false
        }

        should("entity-scoped principal uses role names not entity type for assignability") {
            val principalService = mockk<PrincipalService>()
            val principal = mockk<PrincipalContext>()
            every { principal.getGlobalRoles() } returns emptyList()
            every { principal.getEntityRoles() } returns listOf(
                EntityRole("partner", listOf("partner.admin"), listOf("partner-123"))
            )
            every { principalService.getPrincipal() } returns principal
            every { principalService.ensurePrincipal() } returns principal

            val service = DelegatingAuthzService(principalService, roleService, mockPermissionService)

            service.principalCanAssignRole("partner.admin") shouldBe true
            service.principalCanAssignRole("partner.user") shouldBe true
            service.principalCanAssignRole("merchant.admin") shouldBe true
            service.principalCanAssignRole("merchant.user") shouldBe false
            service.principalCanAssignRole("backoffice.admin") shouldBe false
        }

        should("merchant admin can assign merchant roles only") {
            val principalService = mockk<PrincipalService>()
            val principal = mockk<PrincipalContext>()
            every { principal.getGlobalRoles() } returns emptyList()
            every { principal.getEntityRoles() } returns listOf(
                EntityRole("merchant", listOf("merchant.admin"), listOf("merchant-456"))
            )
            every { principalService.getPrincipal() } returns principal
            every { principalService.ensurePrincipal() } returns principal

            val service = DelegatingAuthzService(principalService, roleService, mockPermissionService)

            service.principalCanAssignRole("merchant.admin") shouldBe true
            service.principalCanAssignRole("merchant.user") shouldBe true
            service.principalCanAssignRole("partner.admin") shouldBe false
            service.principalCanAssignRole("partner.user") shouldBe false
        }

        should("ensureRequestedRolesAreAssignable throws for non-assignable role") {
            val principalService = mockk<PrincipalService>()
            val principal = mockk<PrincipalContext>()
            every { principal.getGlobalRoles() } returns emptyList()
            every { principal.getEntityRoles() } returns listOf(
                EntityRole("merchant", listOf("merchant.admin"), listOf("merchant-456"))
            )
            every { principalService.getPrincipal() } returns principal
            every { principalService.ensurePrincipal() } returns principal

            val service = DelegatingAuthzService(principalService, roleService, mockPermissionService)

            shouldThrow<ForbiddenException> {
                service.ensureRequestedRolesAreAssignable(listOf("partner.admin"))
            }
        }

        should("principal with no roles cannot assign any role") {
            val principalService = mockk<PrincipalService>()
            val principal = mockk<PrincipalContext>()
            every { principal.getGlobalRoles() } returns emptyList()
            every { principal.getEntityRoles() } returns emptyList()
            every { principalService.getPrincipal() } returns principal
            every { principalService.ensurePrincipal() } returns principal

            val service = DelegatingAuthzService(principalService, roleService, mockPermissionService)

            service.principalCanAssignRole("partner.admin") shouldBe false
            service.principalCanAssignRole("merchant.user") shouldBe false
        }
    }
})