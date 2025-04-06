package org.incept5.authz.core.context

import org.incept5.authz.core.model.EntityRole
import java.util.UUID

/**
 * This tells us what global roles and specific
 * entity roles the user or api key/client has
 */
interface PrincipalContext {

    // could be user id or api key id etc
    fun getPrincipalId(): UUID

    fun getGlobalRoles(): List<String>

    fun getEntityRoles(): List<EntityRole>

}