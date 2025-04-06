package org.incept5.authz.quarkus.filter

/**
 * Contribute to the set of regexes that should be ignored by the authz filter
 *
 * an example of a regex to ignore is "/auth/public-keys" to ignore the "/auth/public-keys" resource
 *
 * or "/auth/.*" to ignore all sub-paths of the /auth/ resource
 *
 */
interface IgnoreAuthzFilterProvider {

    fun ignoreRegexes(): List<String>

}