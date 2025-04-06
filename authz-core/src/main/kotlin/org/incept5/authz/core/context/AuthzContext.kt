package org.incept5.authz.core.context

import org.incept5.authz.core.model.Permission
import java.util.*

/**
 * This is a main context object used for authorization
 * check logic. It is available from an {@link AccessControlContext}
 * by calling the ctx.getAuthzContext() method
 */
interface AuthzContext {

    /**
     * Grab the current [PrincipalContext] which will come from
     * the contents of a JWT usually
     * @return the PrincipalRolesContext or null if not authenticated
     */
    fun getPrincipal(): PrincipalContext?

    /**
     * Grab the current [PrincipalContext] which will come from
     * the contents of a JWT usually
     * @return the PrincipalRolesContext or throw an exception if not authenticated
     */
    fun ensurePrincipal(): PrincipalContext

    /**
     * Does the current principal have Global Access to the required permission
     * @param required
     * @return true if the current principal is allowed to do the operation globally
     */
    fun principalHasGlobalPermission(required: Permission): Boolean

    /**
     * Checks that the current principal has Global Access to the required permission
     * @param required
     * @throws 403 exception if the principal does not
     */
    fun ensurePrincipalHasGlobalPermission(required: Permission)

    /**
     * Does the principal have global or entity based access to the specific entity with the specific
     * operation
     * @param required
     * @param type
     * @param entityId
     * @return true if the principal has either global or entity based access
     */
    fun principalHasPermission(required: Permission, type: String, entityId: String): Boolean

    /**
     * Does the principal have global or entity based access to at least one of the specified entities
     * @param required
     * @param type
     * @param possibleEntityIds
     * @return
     */
    fun principalHasPermission(required: Permission, type: String, possibleEntityIds: Collection<String>): Boolean

    /**
     * Checks that the principal has global or entity based access to the specific entity with the specific
     * operation
     * @param required
     * @param type
     * @param entityId
     * @throws 403 exception if the principal does not
     */
    fun ensurePrincipalHasPermission(required: Permission, type: String, entityId: String)

    /**
     * Checks that the principal has global or entity based access to at least one of the specified entities
     * @param required
     * @param type
     * @param possibleEntityIds - set of possible entity ids to match on (any match will do)
     * @throws 403 exception if the principal does not
     */
    fun ensurePrincipalHasPermission(required: Permission, type: String, possibleEntityIds: Collection<String>)

    /**
     * Grab the set of entity ids that the current authenticated principal
     * has access to for the given permission and entity type
     * NOTE: before calling this you should check to see if principalHasGlobalPermission and if they
     * do then you should not limit the query by entity id
     * @param permission
     * @param entityType
     * @return a set of specific entity ids the principal has access to for a specific permission
     */
    fun specificEntityIds(permission: Permission, entityType: String): List<String>

    /**
     * Does the principal have permission to do the requested operation at all - does not check entity matching
     * This should only be used for pre-checks that will later be refined using specificEntityIds operation
     * to limit the query in some way
     * @param required
     * @return true if the principal has global permission or any entity permission for the requested operation
     */
    fun isOperationAllowedForPrincipal(required: Permission): Boolean

    /**
     * Does the principal have permission to do the requested operation at all - does not check entity matching
     * This should only be used for pre-checks that will later be refined using specificEntityIds operation
     * to limit the query in some way
     * @param required
     * @return 403 exception if the principal does not
     */
    fun ensureOperationAllowedForPrincipal(required: Permission)

    /**
     * See if the principal has any entity role of the given type
     * @param type
     * @return
     */
    fun principalHasEntityRole(type: String): Boolean

    /**
     * If we have a populated principal context then we are authenticated
     */
    fun isPrincipalAuthenticated(): Boolean {
        return getPrincipal() != null
    }

    /**
     * can the principal assign the roles to the principal requesting the roles
     * @param requestedRoles the roles being requested
     */
    fun ensureRequestedRolesAreAssignable(roleNames: List<String>)

    // useful for debugging
    fun listGlobalPermissionsForPrincipal(): List<Permission>
    fun listEntityPermissionsForPrincipal(): List<Permission>
}