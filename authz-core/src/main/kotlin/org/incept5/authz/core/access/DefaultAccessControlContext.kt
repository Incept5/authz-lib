package org.incept5.authz.core.access

import org.incept5.authz.core.context.AuthzContext

data class DefaultAccessControlContext(private val args: Array<Any>, private val authzContext: AuthzContext) :
    org.incept5.authz.core.access.AccessControlContext {
    override fun args(): Array<Any> {
        return args
    }

    override fun authz(): AuthzContext {
        return authzContext
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as org.incept5.authz.core.access.DefaultAccessControlContext

        if (!args.contentEquals(other.args)) return false
        return authzContext == other.authzContext
    }

    override fun hashCode(): Int {
        var result = args.contentHashCode()
        result = 31 * result + authzContext.hashCode()
        return result
    }
}