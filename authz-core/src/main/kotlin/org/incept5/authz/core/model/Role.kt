package org.incept5.authz.core.model

/**
 * Assignable roles are roles that can be assigned to a user by a user with the role
 */
data class Role(
    var name: String,
    var permissions: List<String>,
    var extendsRole: String? = null,
    var assignableRoles: List<String> = emptyList()
)
