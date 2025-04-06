package org.incept5.authz.quarkus.interceptor

import jakarta.interceptor.InterceptorBinding

/**
 * Mark your bean for authorization checks by adding this to the class level
 * Then mark each method with [AuthzCheck] to run permission checks before and after the method is invoked
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.CLASS)
@InterceptorBinding
annotation class Authorized
