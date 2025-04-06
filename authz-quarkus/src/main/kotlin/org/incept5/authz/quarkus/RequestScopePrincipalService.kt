package org.incept5.authz.quarkus

import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.service.PrincipalService
import jakarta.enterprise.context.RequestScoped

@RequestScoped
class RequestScopePrincipalService : PrincipalService {

    private var principal: PrincipalContext? = null

    fun setPrincipal(principal: PrincipalContext) {
        this.principal = principal
    }

    override fun getPrincipal(): PrincipalContext? {
        return principal
    }

    override fun ensurePrincipal(): PrincipalContext {
        return principal ?: throw RuntimeException("No principal set")
    }
}