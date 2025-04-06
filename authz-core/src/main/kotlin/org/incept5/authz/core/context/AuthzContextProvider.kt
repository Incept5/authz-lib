package org.incept5.authz.core.context

interface AuthzContextProvider {

    fun authz(): AuthzContext

}