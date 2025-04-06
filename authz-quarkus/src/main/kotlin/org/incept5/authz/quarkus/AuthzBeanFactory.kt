package org.incept5.authz.quarkus

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.enterprise.context.RequestScoped
import jakarta.enterprise.inject.Instance
import jakarta.enterprise.inject.Produces
import jakarta.inject.Singleton
import org.incept5.authz.core.context.AuthzContext
import org.incept5.authz.core.model.Role
import org.incept5.authz.core.service.*
import org.incept5.authz.core.service.simple.DelegatingAuthzService
import org.incept5.authz.core.service.simple.SimplePermissionService
import org.incept5.authz.core.service.simple.SimpleRoleService
import org.incept5.authz.quarkus.config.AuthzConfig
import org.incept5.authz.quarkus.filter.FilterDecision
import org.incept5.authz.quarkus.filter.IgnoreAuthzFilterProvider

// logger
private val logger = KotlinLogging.logger {}

/**
 * Construct the services we need for access control
 */
class AuthzBeanFactory {

    @Produces
    @RequestScoped
    fun filterDecision(
        config: AuthzConfig,
        providers: Instance<IgnoreAuthzFilterProvider>
    ): FilterDecision {
        return FilterDecision(config.filter().ignorePaths(), providers)
    }

    @Produces
    @Singleton
    fun combinedRoleList(config: AuthzConfig, providers: Instance<RolesProvider>): List<Role> {
        val configList = config.roles().map {
            Role(
                it.name(),
                it.permissions(),
                it.extendsRole().orElse(null),
                it.assignableRoles().orElse(emptyList())
            )
        }
        val combinedRoles = mutableListOf<Role>()
        if ( configList.isNotEmpty() ) combinedRoles.addAll(configList)
        providers.forEach { combinedRoles.addAll(it.provideRoles()) }
        logger.debug { "Using roles: $combinedRoles"}
        return combinedRoles
    }

    @Produces
    @Singleton
    fun permissionService(roles: List<Role>): PermissionService {
        return SimplePermissionService(roles)
    }

    @Produces
    @Singleton
    fun roleService(roles: List<Role>): RoleService {
        return SimpleRoleService(roles)
    }

    @Produces
    @Singleton
    fun defaultAuthzContext(permissionService: PermissionService, roleService: RoleService, principalService: PrincipalService): AuthzContext {
        return DelegatingAuthzService(principalService, roleService, permissionService)
    }

    @Produces
    @Singleton
    fun tokenExchangeService(plugins: Instance<TokenExchangePlugin>): TokenExchangeService {
        return PluggableTokenExchangeService(plugins.toList())
    }
}

