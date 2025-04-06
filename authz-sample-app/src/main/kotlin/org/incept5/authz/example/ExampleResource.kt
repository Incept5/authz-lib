package org.incept5.authz.example

import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.core.Response

@Path("/example")
class ExampleResource(private val service: ExampleSecureService) {

    @GET
    @Path("/secure/{id}")
    fun secure(@PathParam("id") id: String): Response {
        val result = service.authorizedMethod(id)
        return Response.ok(result).build()
    }

}


