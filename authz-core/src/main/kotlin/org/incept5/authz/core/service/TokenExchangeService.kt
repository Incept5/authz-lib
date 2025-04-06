package org.incept5.authz.core.service

import org.incept5.authz.core.context.PrincipalContext

/**
 * Exchange in incoming access token for a PrincipalRolesContext
 * which contains the principal id and the roles the principal has
 *
 * The PrincipalRolesContext will then be combined with the Role Map
 * to determine which permissions the principal has been granted
 *
 */
interface TokenExchangeService {
    fun exchangeToken(token: String): PrincipalContext
}