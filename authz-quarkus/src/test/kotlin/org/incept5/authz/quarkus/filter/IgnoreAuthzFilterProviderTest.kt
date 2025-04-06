package org.incept5.authz.quarkus.filter

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import jakarta.enterprise.context.ApplicationScoped

class IgnoreAuthzFilterProviderTest {

    @Test
    fun `should provide ignore paths`() {
        // given
        val provider = TestIgnoreAuthzFilterProvider()
        
        // when
        val ignoreRegexes = provider.ignoreRegexes()
        
        // then
        assertEquals(2, ignoreRegexes.size)
        assertEquals("/test/path1", ignoreRegexes[0])
        assertEquals("/test/path2", ignoreRegexes[1])
    }
    
    @ApplicationScoped
    class TestIgnoreAuthzFilterProvider : IgnoreAuthzFilterProvider {
        override fun ignoreRegexes(): List<String> {
            return listOf("/test/path1", "/test/path2")
        }
    }
}