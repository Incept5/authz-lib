package org.incept5.authz.core.context

/**
 * Holds the current authz context for the current thread
 */
object PrincipalContextHolder {
    private val principals = ThreadLocal<PrincipalContext>()

    fun setPrincipalContext(ctx: PrincipalContext) {
        principals.set(ctx)
    }

    fun getPrincipalContext(): PrincipalContext? {
        return principals.get()
    }

    fun ensurePrincipalContext(): PrincipalContext {
        return getPrincipalContext() ?: throw RuntimeException("The current thread is missing a principal context")
    }

    fun clearPrincipalContext() {
        principals.remove()
    }
}
