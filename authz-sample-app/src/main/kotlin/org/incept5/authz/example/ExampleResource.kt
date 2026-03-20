package org.incept5.authz.example

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.core.SecurityContext
import org.incept5.authz.core.context.PrincipalContext

@Path("/example")
class ExampleResource(private val service: ExampleSecureService) {

    @GET
    @Path("/secure/{id}")
    fun secure(@PathParam("id") id: String): Response {
        val result = service.authorizedMethod(id)
        return Response.ok(result).build()
    }

    /**
     * Example showing how to access the authenticated principal from the
     * JAX-RS SecurityContext. The AuthzFilter sets this automatically.
     *
     * In services that use platform-core-lib, the principal will be an
     * ApiPrincipal which can be cast to access additional fields:
     *
     *     val apiPrincipal = securityContext.userPrincipal as ApiPrincipal
     *     apiPrincipal.subject     // user/client subject
     *     apiPrincipal.userRole    // UserRole from the token
     *     apiPrincipal.entityType  // e.g. "partner", "merchant"
     *     apiPrincipal.entityId    // entity ID
     *     apiPrincipal.scopes      // OAuth scopes
     *     apiPrincipal.clientId    // OAuth client ID
     */
    @GET
    @Path("/me")
    fun me(@Context securityContext: SecurityContext): Response {
        val principal = securityContext.userPrincipal as PrincipalContext
        return Response.ok(
            mapOf(
                "principalId" to principal.getPrincipalId(),
                "name" to principal.name,
                "globalRoles" to principal.getGlobalRoles(),
                "entityRoles" to principal.getEntityRoles(),
                "isAdmin" to securityContext.isUserInRole("backoffice.admin")
            )
        ).build()
    }
}
