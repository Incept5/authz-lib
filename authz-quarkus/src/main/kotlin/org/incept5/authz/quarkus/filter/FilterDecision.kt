package org.incept5.authz.quarkus.filter

import jakarta.enterprise.inject.Instance

/**
 * Decide if the request should be filtered or not
 */
class FilterDecision(
    ignorePatterns: List<String>,
    providers: Instance<IgnoreAuthzFilterProvider>,
) {

    // Compiled once at construction. Pattern semantics (`*` and `{segment}`) live in the shared
    // [PathPatternMatcher], reused by the MFA-skip list so the two path lists cannot drift apart.
    private val excludeList: List<Regex> =
        PathPatternMatcher.compileAll(ignorePatterns + providers.flatMap { it.ignoreRegexes() })

    fun shouldIgnore(path: String): Boolean {
        return excludeList.any { it.matches(path) }
    }
}
