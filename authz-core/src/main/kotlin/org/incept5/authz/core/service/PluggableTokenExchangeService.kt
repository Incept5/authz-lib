package org.incept5.authz.core.service

import org.incept5.authz.core.context.PrincipalContext
import org.incept5.authz.core.exp.AuthnException
import org.incept5.authz.core.exp.AuthzErrorCodes
import org.incept5.authz.core.exp.AuthzException

/**
 * Run through the list of token exchange plugins and return the first
 * non null PrincipalContext returned by the plugin
 */
class PluggableTokenExchangeService(private val plugins: List<TokenExchangePlugin>) : TokenExchangeService {

        override fun exchangeToken(token: String): PrincipalContext {
            for (plugin in plugins) {
                val context = plugin.exchangeToken(token)
                if (context != null) {
                    return context
                }
            }
            throw AuthnException(AuthzErrorCodes.INVALID_TOKEN, "Invalid bearer token: $token")
        }
}