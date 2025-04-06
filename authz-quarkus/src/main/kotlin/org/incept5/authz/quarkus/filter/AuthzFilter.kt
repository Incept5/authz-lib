package org.incept5.authz.quarkus.filter

import org.incept5.authz.core.context.PrincipalContextHolder
import org.incept5.authz.core.service.TokenExchangeService
import org.incept5.authz.quarkus.RequestScopePrincipalService
import io.quarkus.logging.Log
import jakarta.annotation.Priority
import jakarta.ws.rs.Priorities
import jakarta.ws.rs.container.ContainerRequestContext
import jakarta.ws.rs.container.ContainerRequestFilter
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.Provider

private const val AUTHORIZATION = "Authorization"
private const val BEARER_ = "Bearer "

/**
 * This request filter should intercept all requests (except those specifically ignored),
 * exchange the auth token for a principal context, and then add it to the propagation context
 * so that it is available to all downstream services
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
class AuthzFilter (
    private val filterDecision: FilterDecision,
    private val principalService: RequestScopePrincipalService,
    private val tokenExchangeService: TokenExchangeService
) : ContainerRequestFilter {

    override fun filter(requestContext: ContainerRequestContext) {
        if (filterDecision.shouldIgnore(requestContext.uriInfo.path)) {
            Log.trace { "Ignoring request to ${requestContext.uriInfo.path} because it is in the list of ignored paths" }
            return
        }

        val authHeader = requestContext.getHeaderString(AUTHORIZATION)
        if (authHeader == null || !authHeader.startsWith(BEARER_)) {
            Log.warn("No auth header found or it is not a bearer token")
            requestContext.abortWith(Response.status(Response.Status.UNAUTHORIZED).build())
            return
        }

        val authToken = authHeader.replace(BEARER_, "")
        Log.debug ("Exchanging auth token $authToken for principal context")
        val principalContext = tokenExchangeService.exchangeToken(authToken)
        Log.debug ("Adding principal context $principalContext to propagation context")
        // Assuming you have a way to store the principal context for downstream services
        // E.g., using a thread-local storage or context propagation mechanism
        principalService.setPrincipal(principalContext)
    }
}
