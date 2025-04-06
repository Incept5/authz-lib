package org.incept5.authz.quarkus.filter

import io.quarkus.test.junit.QuarkusTest
import jakarta.enterprise.inject.Instance
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class FilterDecisionTest {

    @Test
    fun `should ignore paths in the ignore list`() {
        // given
        val ignorePaths = listOf("/health", "/metrics", "/swagger")
        val providers = mock(Instance::class.java) as Instance<IgnoreAuthzFilterProvider>
        `when`(providers.iterator()).thenReturn(mutableListOf<IgnoreAuthzFilterProvider>().iterator())
        
        val filterDecision = FilterDecision(ignorePaths, providers)
        
        // when/then
        assertTrue(filterDecision.shouldIgnore("/health"))
        assertTrue(filterDecision.shouldIgnore("/metrics"))
        assertTrue(filterDecision.shouldIgnore("/swagger"))
        assertFalse(filterDecision.shouldIgnore("/api/users"))
    }
    
    @Test
    fun `should ignore paths from providers`() {
        // given
        val ignorePaths = listOf("/health")
        val provider = mock(IgnoreAuthzFilterProvider::class.java)
        `when`(provider.ignoreRegexes()).thenReturn(listOf("/custom/path"))
        
        val providers = mock(Instance::class.java) as Instance<IgnoreAuthzFilterProvider>
        `when`(providers.iterator()).thenReturn(mutableListOf(provider).iterator())
        
        val filterDecision = FilterDecision(ignorePaths, providers)
        
        // when/then
        assertTrue(filterDecision.shouldIgnore("/health"))
        assertTrue(filterDecision.shouldIgnore("/custom/path"))
        assertFalse(filterDecision.shouldIgnore("/api/users"))
    }
    
    @Test
    fun `should match path patterns`() {
        // given
        val ignorePaths = listOf("/health", "/api/v1/public/*")
        val providers = mock(Instance::class.java) as Instance<IgnoreAuthzFilterProvider>
        `when`(providers.iterator()).thenReturn(mutableListOf<IgnoreAuthzFilterProvider>().iterator())
        
        val filterDecision = FilterDecision(ignorePaths, providers)
        
        // when/then
        assertTrue(filterDecision.shouldIgnore("/health"))
        assertTrue(filterDecision.shouldIgnore("/api/v1/public/users"))
        assertTrue(filterDecision.shouldIgnore("/api/v1/public/health"))
        assertFalse(filterDecision.shouldIgnore("/api/v1/private/users"))
    }
}