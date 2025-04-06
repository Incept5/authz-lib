package org.incept5.authz.core.service

import org.incept5.authz.core.model.Role

/**
 * Allow arbitrary components to add to the set of roles supported
 * by the application
 */
interface RolesProvider {

    fun provideRoles(): List<Role>

}