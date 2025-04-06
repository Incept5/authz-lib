package org.incept5.authz.quarkus

import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.context.PrincipalContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class RequestScopePrincipalServiceTest {

    private lateinit var principalService: RequestScopePrincipalService

    @BeforeEach
    fun setup() {
        principalService = RequestScopePrincipalService()
    }

    @Test
    fun `should return null when no principal is set`() {
        // when
        val result = principalService.getPrincipal()

        // then
        assertNull(result)
    }

    @Test
    fun `should return principal when set`() {
        // given
        val principalId = UUID.randomUUID()
        val principal = DefaultPrincipalContext(
            principalId = principalId,
            globalRoles = listOf("admin"),
            entityRoles = emptyList()
        )

        // when
        principalService.setPrincipal(principal)
        val result = principalService.getPrincipal()

        // then
        assertEquals(principal, result)
        assertEquals(principalId, result?.getPrincipalId())
        assertEquals(listOf("admin"), result?.getGlobalRoles())
    }

    @Test
    fun `should override principal when set multiple times`() {
        // given
        val principal1 = DefaultPrincipalContext(
            principalId = UUID.randomUUID(),
            globalRoles = listOf("admin"),
            entityRoles = emptyList()
        )
        val principal2 = DefaultPrincipalContext(
            principalId = UUID.randomUUID(),
            globalRoles = listOf("user"),
            entityRoles = emptyList()
        )

        // when
        principalService.setPrincipal(principal1)
        principalService.setPrincipal(principal2)
        val result = principalService.getPrincipal()

        // then
        assertEquals(principal2, result)
        assertEquals(principal2.getPrincipalId(), result?.getPrincipalId())
        assertEquals(listOf("user"), result?.getGlobalRoles())
    }
}