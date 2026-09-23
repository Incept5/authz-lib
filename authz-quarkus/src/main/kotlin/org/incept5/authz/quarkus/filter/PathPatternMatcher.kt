package org.incept5.authz.quarkus.filter

/**
 * Shared path-pattern compiler used by both the authz ignore list ([FilterDecision]) and the
 * MFA-skip list ([AssuranceLevelFilter]). Patterns are NOT regexes — every character is literal
 * except two wildcards:
 *
 *  - `*` matches any run of characters, including `/`.
 *  - `{segment}` matches exactly one non-empty path segment, so
 *    `/api/v1/users/{segment}` matches `/api/v1/users/u_1` but not `/api/v1/users/u_1/totp`.
 *
 * A pattern with neither wildcard matches only its exact literal path. Every other character —
 * including regex metacharacters such as `.`, `+`, `?`, `(`, `[` and JAX-RS-style `{id}`
 * placeholders — is matched literally, so a configured pattern can never fail to compile at
 * request time. Callers compile once (at construction) and keep the [Regex]; matching is then a
 * single anchored match per request.
 */
internal object PathPatternMatcher {

    private const val STAR = "*"

    /** The two wildcard tokens; every run of characters between them is escaped verbatim. */
    private val WILDCARD = Regex("""\*|\{segment}""")

    fun compile(pattern: String): Regex {
        val regex = StringBuilder("^")
        var literalStart = 0
        for (wildcard in WILDCARD.findAll(pattern)) {
            regex.append(literal(pattern.substring(literalStart, wildcard.range.first)))
            regex.append(if (wildcard.value == STAR) ".*" else "[^/]+")
            literalStart = wildcard.range.last + 1
        }
        regex.append(literal(pattern.substring(literalStart)))
        regex.append("$")
        return Regex(regex.toString())
    }

    fun compileAll(patterns: Iterable<String>): List<Regex> = patterns.map(::compile)

    private fun literal(chunk: String): String = if (chunk.isEmpty()) "" else Regex.escape(chunk)
}
