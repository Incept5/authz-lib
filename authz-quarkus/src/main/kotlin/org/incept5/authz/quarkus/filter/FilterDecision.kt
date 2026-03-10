package org.incept5.authz.quarkus.filter

import jakarta.enterprise.inject.Instance

/**
 * Decide if the request should be filtered or not
 */
class FilterDecision (
    ignoreRegexes: List<String>,
    providers: Instance<IgnoreAuthzFilterProvider>)  {

    private val excludeList = mutableListOf<String>()

    // build the list of regexes to ignore once at the beginning
    init {
        excludeList.addAll(ignoreRegexes)
        providers.forEach { excludeList.addAll(it.ignoreRegexes()) }
    }

    fun shouldIgnore(path: String): Boolean {
        return excludeList.any { matchesPattern(it, path) }
    }
    
    /**
     * Matches a path against a pattern, supporting wildcard (*) characters
     */
    private fun matchesPattern(pattern: String, path: String): Boolean {
        // Direct match
        if (pattern == path) {
            return true
        }
        
        // Handle wildcard patterns
        if (pattern.contains("*")) {
            val regex = pattern
                .replace(".", "\\.")  // Escape dots
                .replace("*", ".*")   // Convert * to .*
                .let { "^$it$" }      // Anchor pattern
                .toRegex()
                
            return regex.matches(path)
        }
        
        return false
    }
}