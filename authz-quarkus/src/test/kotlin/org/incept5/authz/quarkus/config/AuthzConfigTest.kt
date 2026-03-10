package org.incept5.authz.quarkus.config

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.Optional

class AuthzConfigTest {

    @Test
    fun `should return filter configuration`() {
        // given
        val filterConfig = mock(FilterConfig::class.java)
        val config = object : AuthzConfig {
            override fun filter(): FilterConfig = filterConfig
            override fun roles(): List<RoleConfig> = emptyList()
            override fun users(): List<UserConfig> = emptyList()
        }

        // when
        val result = config.filter()

        // then
        assertEquals(filterConfig, result)
    }

    @Test
    fun `should return roles configuration`() {
        // given
        val role1 = mock(RoleConfig::class.java)
        val role2 = mock(RoleConfig::class.java)
        val roles = listOf(role1, role2)
        
        val config = object : AuthzConfig {
            override fun filter(): FilterConfig = mock(FilterConfig::class.java)
            override fun roles(): List<RoleConfig> = roles
            override fun users(): List<UserConfig> = emptyList()
        }

        // when
        val result = config.roles()

        // then
        assertEquals(roles, result)
        assertEquals(2, result.size)
    }

    @Test
    fun `should handle role configuration`() {
        // given
        val roleName = "admin"
        val permissions = listOf("read", "write", "delete")
        val extendsRole = "user"
        val assignableRoles = listOf("editor", "viewer")
        
        val role = object : RoleConfig {
            override fun name(): String = roleName
            override fun permissions(): List<String> = permissions
            override fun extendsRole(): Optional<String> = Optional.of(extendsRole)
            override fun assignableRoles(): Optional<List<String>> = Optional.of(assignableRoles)
        }

        // when/then
        assertEquals(roleName, role.name())
        assertEquals(permissions, role.permissions())
        assertTrue(role.extendsRole().isPresent)
        assertEquals(extendsRole, role.extendsRole().get())
        assertTrue(role.assignableRoles().isPresent)
        assertEquals(assignableRoles, role.assignableRoles().get())
    }

    @Test
    fun `should handle filter configuration`() {
        // given
        val ignorePaths = listOf("/health", "/metrics", "/swagger")
        
        val filter = object : FilterConfig {
            override fun ignorePaths(): List<String> = ignorePaths
        }

        // when/then
        assertEquals(ignorePaths, filter.ignorePaths())
    }
}