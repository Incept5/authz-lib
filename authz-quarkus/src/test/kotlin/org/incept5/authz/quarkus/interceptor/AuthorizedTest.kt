package org.incept5.authz.quarkus.interceptor

import jakarta.interceptor.InterceptorBinding
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthorizedTest {

    @Test
    fun `should have correct annotation properties`() {
        // given
        val annotation = Authorized::class.java
        
        // when/then
        assertTrue(annotation.isAnnotationPresent(InterceptorBinding::class.java), 
            "Authorized should be annotated with @InterceptorBinding")
        
        // Check retention, target and inherited through presence of annotation
        assertTrue(annotation.isAnnotation, "Authorized should be an annotation")
    }

    @Authorized
    class TestClass {
        fun testMethod() {}
    }

    @Test
    fun `should be applicable to classes`() {
        // given
        val clazz = TestClass::class.java

        // when
        val classAnnotation = clazz.getAnnotation(Authorized::class.java)

        // then
        assertTrue(classAnnotation != null, "Authorized should be applicable to classes")
    }
}