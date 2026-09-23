package org.incept5.authz.quarkus.config

import io.smallrye.config.ConfigMapping
import java.util.Optional

/**
 * In the application.yaml file, the roles are defined under the incept5.authz prefix like:
 *
 * incept5:
 *   authz:
 *     roles:
 *       - name: backoffice.admin
 *         permissions:
 *           - example.get
 *
 *     users:
 *       - username: backoffice-admin@incept5.com
 *         password: password
 *         roles:
 *           - backoffice.admin
 *
 */
@ConfigMapping(prefix = "incept5.authz")
interface AuthzConfig {

    fun roles(): List<RoleConfig>

    fun filter(): FilterConfig

    fun mfa(): MfaConfig

    fun users(): List<UserConfig>
}

interface FilterConfig {
    fun ignorePaths(): List<String>
}

/**
 * Multi-factor enforcement, read by [org.incept5.authz.quarkus.filter.AssuranceLevelFilter].
 *
 * Both lists are `Optional` and absent by default, so authz-lib is behaviour-neutral until a
 * consuming application opts in. Nested under the already-registered [AuthzConfig] mapping (rather
 * than a standalone `@ConfigMapping`, which Quarkus does not auto-register from a library jar), so
 * it rides on the same `incept5.authz` registration and existing consumers that set no
 * `incept5.authz.mfa` block are unaffected.
 *
 * ```
 * incept5:
 *   authz:
 *     mfa:
 *       required-roles: backoffice.admin
 *       skip-paths: /api/v1/users/profile
 * ```
 */
interface MfaConfig {

    /**
     * Role names whose principals must hold a multi-factor session. Empty/absent disables
     * enforcement. Compared against the principal's global and entity roles after expanding
     * `extends-role` inheritance, so a role that extends a required role is governed too.
     * Provider-agnostic —
     * the consuming application supplies its own privileged roles (FanFair defaults this to
     * `backoffice.admin`, story AC2); authz-lib hardcodes no role name.
     */
    fun requiredRoles(): Optional<List<String>>

    /**
     * Path patterns (same `*` / `{segment}` wildcards as the ignore list; every other character is
     * literal) that a single-factor holder of a required role may still reach — the endpoints
     * needed to enrol a second factor. Distinct from the ignore list: these still authenticate,
     * they only skip the MFA gate.
     */
    fun skipPaths(): Optional<List<String>>
}

interface RoleConfig {
    fun name(): String
    fun permissions(): List<String>
    fun extendsRole(): Optional<String>
    fun assignableRoles(): Optional<List<String>>
}

interface UsersConfig {
    fun globalUsers(): List<UserConfig>
}

interface UserConfig {
    fun username(): String
    fun password(): String
    fun roles(): List<String>
}