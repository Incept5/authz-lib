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

    fun users(): List<UserConfig>
}

interface FilterConfig {
    fun ignorePaths(): List<String>
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