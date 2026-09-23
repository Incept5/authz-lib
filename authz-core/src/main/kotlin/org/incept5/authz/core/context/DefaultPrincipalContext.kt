package org.incept5.authz.core.context

import org.incept5.authz.core.model.EntityRole
import java.util.UUID

/**
 * Plain data-class [PrincipalContext]. [assuranceLevel] and [machinePrincipal] default to the
 * interface defaults (single-factor, human) so existing call sites are unaffected; token-exchange
 * plugins and tests that need a multi-factor or machine principal set them explicitly.
 */
data class DefaultPrincipalContext @JvmOverloads constructor(
    private val name: String,
    private val principalId: UUID,
    private val globalRoles: List<String>,
    private val entityRoles: List<EntityRole> = emptyList(),
    private val assuranceLevel: AssuranceLevel = AssuranceLevel.SINGLE_FACTOR,
    private val machinePrincipal: Boolean = false,
) : PrincipalContext {

    override fun getPrincipalId(): UUID {
        return principalId
    }

    override fun getGlobalRoles(): List<String> {
        return globalRoles
    }

    override fun getEntityRoles(): List<EntityRole> {
        return entityRoles
    }

    override fun getName(): String {
        return name
    }

    override fun getAssuranceLevel(): AssuranceLevel {
        return assuranceLevel
    }

    override fun isMachinePrincipal(): Boolean {
        return machinePrincipal
    }
}
