package org.incept5.authz.quarkus.filter

import org.incept5.authz.core.context.DefaultPrincipalContext
import org.incept5.authz.core.service.TokenExchangeService
import org.incept5.authz.quarkus.RequestScopePrincipalService
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.UriInfo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.*
import java.util.UUID

class AuthzFilterTest {

    private lateinit var filterDecision: FilterDecision
    private lateinit var principalService: RequestScopePrincipalService
    private lateinit var tokenExchangeService: TokenExchangeService
    private lateinit var authzFilter: AuthzFilter
    private lateinit var requestContext: ContainerRequestContext
    private lateinit var uriInfo: UriInfo

    @BeforeEach
    fun setup() {
        filterDecision = mock(FilterDecision::class.java)
        principalService = mock(RequestScopePrincipalService::class.java)
        tokenExchangeService = mock(TokenExchangeService::class.java)
        requestContext = mock(ContainerRequestContext::class.java)
        uriInfo = mock(UriInfo::class.java)
        
        `when`(requestContext.uriInfo).thenReturn(uriInfo)
        
        authzFilter = AuthzFilter(filterDecision, principalService, tokenExchangeService)
    }
    
    @Test
    fun `should ignore request if path is in ignore list`() {
        // given
        val path = "/health"
        `when`(uriInfo.path).thenReturn(path)
        `when`(filterDecision.shouldIgnore(path)).thenReturn(true)
        
        // when
        authzFilter.filter(requestContext)
        
        // then
        verify(filterDecision).shouldIgnore(path)
        verifyNoInteractions(tokenExchangeService)
        verifyNoInteractions(principalService)
        verify(requestContext, never()).abortWith(any())
    }
    
    @Test
    fun `should abort with 401 if no auth header`() {
        // given
        val path = "/api/users"
        `when`(uriInfo.path).thenReturn(path)
        `when`(filterDecision.shouldIgnore(path)).thenReturn(false)
        `when`(requestContext.getHeaderString("Authorization")).thenReturn(null)
        
        // when
        authzFilter.filter(requestContext)
        
        // then
        val responseCaptor = ArgumentCaptor.forClass(Response::class.java)
        verify(requestContext).abortWith(responseCaptor.capture())
        assertEquals(Response.Status.UNAUTHORIZED.statusCode, responseCaptor.value.status)
        verifyNoInteractions(tokenExchangeService)
        verifyNoInteractions(principalService)
    }
    
    @Test
    fun `should abort with 401 if auth header is not bearer token`() {
        // given
        val path = "/api/users"
        `when`(uriInfo.path).thenReturn(path)
        `when`(filterDecision.shouldIgnore(path)).thenReturn(false)
        `when`(requestContext.getHeaderString("Authorization")).thenReturn("Basic dXNlcjpwYXNz")
        
        // when
        authzFilter.filter(requestContext)
        
        // then
        val responseCaptor = ArgumentCaptor.forClass(Response::class.java)
        verify(requestContext).abortWith(responseCaptor.capture())
        assertEquals(Response.Status.UNAUTHORIZED.statusCode, responseCaptor.value.status)
        verifyNoInteractions(tokenExchangeService)
        verifyNoInteractions(principalService)
    }
    
    @Test
    fun `should exchange token and set principal context`() {
        // given
        val path = "/api/users"
        val token = "valid-token"
        val principalContext = DefaultPrincipalContext(
            principalId = UUID.randomUUID(),
            globalRoles = listOf("user"),
            entityRoles = emptyList()
        )
        
        `when`(uriInfo.path).thenReturn(path)
        `when`(filterDecision.shouldIgnore(path)).thenReturn(false)
        `when`(requestContext.getHeaderString("Authorization")).thenReturn("Bearer $token")
        `when`(tokenExchangeService.exchangeToken(token)).thenReturn(principalContext)
        
        // when
        authzFilter.filter(requestContext)
        
        // then
        verify(tokenExchangeService).exchangeToken(token)
        verify(principalService).setPrincipal(principalContext)
        verify(requestContext, never()).abortWith(any())
    }
}