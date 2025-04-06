package org.incept5.authz.core.annotation

import org.incept5.authz.core.access.AccessControl
import kotlin.reflect.KClass

/**
 * Run permission check logic before AND after the annotated method is invoked
 */
@MustBeDocumented
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION)
annotation class AuthzCheck(val value: KClass<out AccessControl<*>>)
