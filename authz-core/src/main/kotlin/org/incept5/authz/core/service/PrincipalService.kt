package org.incept5.authz.core.service

import org.incept5.authz.core.context.PrincipalContext

/**
 * Grab the principal context in play
 */
interface PrincipalService {

    fun getPrincipal(): PrincipalContext?

    fun ensurePrincipal(): PrincipalContext
}