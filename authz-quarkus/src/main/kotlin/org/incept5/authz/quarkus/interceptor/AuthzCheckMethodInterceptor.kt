package org.incept5.authz.quarkus.interceptor

import org.incept5.authz.core.access.AccessControl
import org.incept5.authz.core.access.DefaultAccessControlContext
import org.incept5.authz.core.annotation.AuthzCheck
import org.incept5.authz.core.context.AuthzContext
import io.github.oshai.kotlinlogging.KotlinLogging
import io.quarkus.arc.Arc
import jakarta.annotation.Priority
import jakarta.inject.Inject
import jakarta.interceptor.AroundInvoke
import jakarta.interceptor.Interceptor
import jakarta.interceptor.InvocationContext

private val logger = KotlinLogging.logger {}

/**
 * Interceptor for @AuthzCheck annotations, which will grab the access control class from the annotation
 * and call the before() and after() methods on it around the method call.
 *
 * This is used to check permissions. Access Control beans can be defined in the ApplicationContext with
 * injected collaborators, or they can be simple classes with no dependencies that are created on the fly.
 */
@Interceptor
@Authorized
@Priority(Interceptor.Priority.PLATFORM_AFTER)
class AuthzCheckMethodInterceptor {

    @Inject
    lateinit var authzContext: AuthzContext

    @AroundInvoke
    fun intercept(context: InvocationContext): Any? {
        val annotation = context.method.getAnnotation(AuthzCheck::class.java)
        // Return early if no @AuthzCheck annotation was found.
        if (annotation == null) {
            logger.debug { "No AuthzCheck annotation found on method ${context.method}" }
            return context.proceed()
        }

        val accessControl = findAccessControlBean(annotation)
        val ctx = DefaultAccessControlContext(context.parameters, authzContext)
        logger.debug { "Running access control for $accessControl with context $ctx"}
        accessControl.before(ctx)
        val result = context.proceed()
        return accessControl.after(result, ctx)
    }

    /**
     * 1) Grab the access control class from the AuthzCheck annotation on the running method
     * 2) Look in the ApplicationContext for a bean of that type and use it if found
     * 3) If not found, create a new instance of the class and use that
     */
    @Suppress("UNCHECKED_CAST")
    private fun findAccessControlBean(annotation: AuthzCheck): AccessControl<Any> {
        // we expect to find the annotation on the method, otherwise, the interceptor would not be running
        val accessControlClass = annotation.value.java
        val bean = Arc.container().instance(accessControlClass as Class<AccessControl<Any>>).get()
        if ( bean != null ) {
            logger.debug { "Found access control bean: $bean" }
            return bean
        }
        logger.debug { "No access control bean found for $accessControlClass, creating a new instance" }
        return accessControlClass.getConstructor().newInstance()

    }
}