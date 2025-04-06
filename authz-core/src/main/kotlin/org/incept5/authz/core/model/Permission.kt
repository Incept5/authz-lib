package org.incept5.authz.core.model

import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Encapsulates a permission that a user has to a resource
 *
 * can be constructed from a string like "resource:operation"
 *
 * e.g. users:create or users.pii:read
 *
 */
data class Permission(
    val resource: String,
    val operation: AccessOperation
) {
    companion object {
        val SYSTEM_ALL_GRANTED_PERMISSION = of(".*:all")

        fun of(permission: String): Permission {
            if (!isScopeLike(permission)) {
                throw Exception("Permission string is not scope style with a single colon: $permission")
            }
            return try {
                val firstColon = permission.indexOf(":")
                val resource = permission.substring(0, firstColon)
                Permission(resource, AccessOperation.parse(permission.substring(firstColon + 1)))
            } catch (e: Exception) {
                throw Exception("Invalid permission string encountered: $permission", e)
            }
        }

        /**
         * Returns true if the permission string is in the format of "resource:operation" with a single colon
         */
        fun isScopeLike(permission: String): Boolean {
            return permission.indexOf(':') > -1 && permission.indexOf(':') == permission.lastIndexOf(':')
        }
    }

    fun matches(required: Permission): Boolean {
        val resourceMatches = resource == required.resource || resource == SYSTEM_ALL_GRANTED_PERMISSION.resource
        val operationMatches = operation.matches(required.operation)
        val result = resourceMatches && operationMatches
        logger.trace { "$this matches required $required - $result - resourceMatches: $resourceMatches, operationMatches: $operationMatches" }
        return result
    }

    override fun toString(): String {
        return "$resource:${operation.name.lowercase()}"
    }
}
