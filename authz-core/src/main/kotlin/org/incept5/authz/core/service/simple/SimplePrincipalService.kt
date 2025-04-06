package org.incept5.authz.core.service.simple

import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.context.PrincipalContextHolder
import org.incept5.authz.core.service.PrincipalService

/**
 * Simple implementation of the principal service that
 * just grabs the principal context from the thread local
 */
class SimplePrincipalService : PrincipalService {

    override fun getPrincipal(): PrincipalContext? {
        return PrincipalContextHolder.getPrincipalContext()
    }

    override fun ensurePrincipal(): PrincipalContext {
        return PrincipalContextHolder.ensurePrincipalContext()
    }
}