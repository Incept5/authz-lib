package org.incept5.authz.quarkus.interceptor

import jakarta.interceptor.InvocationContext
import org.incept5.authz.core.annotation.AuthzCheck
import org.incept5.authz.core.context.AuthzContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.lang.reflect.Method

@ExtendWith(MockitoExtension::class)
class AuthzCheckMethodInterceptorTest {

    @Mock
    private lateinit var authzContext: AuthzContext

    @Mock
    private lateinit var invocationContext: InvocationContext

    @Mock
    private lateinit var method: Method

    @InjectMocks
    private lateinit var interceptor: AuthzCheckMethodInterceptor

    @Test
    fun `should proceed without checks if no annotation present`() {
        // given
        `when`(invocationContext.method).thenReturn(method)
        `when`(method.getAnnotation(AuthzCheck::class.java)).thenReturn(null)
        `when`(invocationContext.proceed()).thenReturn("result")

        // when
        val result = interceptor.intercept(invocationContext)

        // then
        assertEquals("result", result)
        verify(invocationContext).proceed()
        verifyNoInteractions(authzContext)
    }

}