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

    @Test
    fun `star wildcard still matches across path separators`() {
        val filterDecision = emptyProviderDecision(listOf("/api/v1/payment-sessions/*"))

        // The existing broad behaviour is unchanged: * spans '/'.
        assertTrue(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC"))
        assertTrue(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC/enhanced"))
    }

    @Test
    fun `segment wildcard matches exactly one non-empty segment`() {
        val filterDecision = emptyProviderDecision(listOf("/api/v1/payment-sessions/{segment}"))

        // matches a single-segment sub-path
        assertTrue(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC"))
        // does NOT reach into a deeper service-only sub-path
        assertFalse(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC/enhanced"))
        // does NOT match the bare prefix or a trailing slash (segment must be non-empty)
        assertFalse(filterDecision.shouldIgnore("/api/v1/payment-sessions"))
        assertFalse(filterDecision.shouldIgnore("/api/v1/payment-sessions/"))
    }

    @Test
    fun `segment wildcard can sit before a fixed sub-path`() {
        val filterDecision = emptyProviderDecision(listOf("/api/v1/payment-sessions/{segment}/confirm"))

        assertTrue(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC/confirm"))
        assertFalse(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC/enhanced"))
        assertFalse(filterDecision.shouldIgnore("/api/v1/payment-sessions/ps_ABC"))
    }

    @Test
    fun `dots in a wildcard pattern are matched literally`() {
        val filterDecision = emptyProviderDecision(listOf("/api/v1/files/*.json"))

        assertTrue(filterDecision.shouldIgnore("/api/v1/files/report.json"))
        // the escaped dot is literal, so 'Xjson' must not match
        assertFalse(filterDecision.shouldIgnore("/api/v1/files/reportXjson"))
    }

    private fun emptyProviderDecision(ignorePaths: List<String>): FilterDecision {
        val providers = mock(Instance::class.java) as Instance<IgnoreAuthzFilterProvider>
        `when`(providers.iterator()).thenReturn(mutableListOf<IgnoreAuthzFilterProvider>().iterator())
        return FilterDecision(ignorePaths, providers)
    }
}