package org.incept5.authz.core.context

import org.incept5.authz.core.model.EntityRole
import java.security.Principal
import java.util.UUID

/**
 * This tells us what global roles and specific
 * entity roles the user or api key/client has
 */
interface PrincipalContext: Principal {

    // could be user id or api key id etc
    fun getPrincipalId(): UUID

    fun getGlobalRoles(): List<String>

    fun getEntityRoles(): List<EntityRole>

    /**
     * How strongly this session was authenticated. Defaults to [AssuranceLevel.SINGLE_FACTOR] so
     * existing implementations keep compiling and behave as before. A token-exchange plugin that
     * understands its provider's assurance claim overrides this; the MFA-enforcement filter reads
     * only this value, never a provider claim.
     */
    fun getAssuranceLevel(): AssuranceLevel = AssuranceLevel.SINGLE_FACTOR

    /**
     * True for a non-human credential — an API key or service-to-service token. MFA enforcement
     * never applies to machine principals, whatever roles they carry. Defaults to false so existing
     * implementations are unaffected; the plugin marks machine-issued tokens.
     */
    fun isMachinePrincipal(): Boolean = false

}