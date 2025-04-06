package org.incept5.authz.core.model

/**
 * Encapsulates how a user is related to an entity and therefore what access they have to it
 *
 * type: probably "org"
 * roles: something like org_admin or org_support etc
 * ids: the ids of the entities that the user has access via the given roles
 */
data class EntityRole(
    val type: String,
    val roles: List<String>,
    val ids: List<String>
) {
    fun matches(type: String, entityId: String): Boolean {
        return this.type == type && ids.contains(entityId)
    }

    fun matches(type: String, possibleEntityIds: Collection<String>): Boolean {
        return this.type == type && possibleEntityIds.any { ids.contains(it) }
    }
}
