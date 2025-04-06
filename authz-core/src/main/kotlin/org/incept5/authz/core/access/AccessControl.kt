package org.incept5.authz.core.access

/**
 * Run permission check logic before AND after the annotated method is invoked
 */
interface AccessControl<R> {

    fun before(ctx: org.incept5.authz.core.access.DefaultAccessControlContext) {}

    fun after(result: R?, ctx: org.incept5.authz.core.access.DefaultAccessControlContext): R? = result
}
