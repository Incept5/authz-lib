# Authz Lib

## Overview

Annotation-driven access control for Quarkus services. Define roles and permissions in YAML, protect methods with `@AuthzCheck`, and let the framework handle token exchange, principal resolution, and permission enforcement.

## Modules

- **authz-core** — framework-agnostic models, interfaces, and permission logic
- **authz-quarkus** — Quarkus integration (request filter, CDI interceptor, request-scoped principal)
- **authz-testing** — mock token exchange plugin for tests

## Installation

Add the JitPack repository and dependencies:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.incept5.authz-lib:authz-core:latest.release")
    implementation("com.github.incept5.authz-lib:authz-quarkus:latest.release")
    testImplementation("com.github.incept5.authz-lib:authz-testing:latest.release")
}
```

## Quick Start

### 1. Define roles and permissions in `application.yaml`

```yaml
authz:
  roles:
    - name: backoffice.admin
      permissions:
        - users:all
        - orgs:all
    - name: org.admin
      permissions:
        - users:read
        - users:create
        - users:update
    - name: org.support
      permissions:
        - users:read
```

### 2. Protect a service method

```kotlin
@Singleton
@Authorized
class ExampleSecureService {

    @AuthzCheck(ExampleAccessControl::class)
    fun authorizedMethod(id: String): String {
        return "Authorized method called with id: $id"
    }
}

class ExampleAccessControl : BaseEntityAccessControl(
    permission = Permission.of("example:read"),
    entityType = "org",
    extractEntityId = { ctx -> ctx.firstArg() }
)
```

### 3. Implement a token exchange plugin

Provide a `TokenExchangePlugin` bean that validates incoming tokens and returns a `PrincipalContext`:

```kotlin
@Singleton
class MyTokenExchangePlugin : TokenExchangePlugin {
    override fun exchangeToken(token: String): PrincipalContext? {
        // Validate the token and return a PrincipalContext, or null if invalid.
        // Return null to let the next plugin in the chain try.
    }
}
```

The `PluggableTokenExchangeService` iterates all registered plugins and returns the first non-null result. If no plugin succeeds, an `AuthnException` is thrown.

## Authentication Flow

The `AuthzFilter` (a JAX-RS `ContainerRequestFilter` at `AUTHENTICATION` priority) handles the request lifecycle:

1. Extracts the Bearer token from the `Authorization` header
2. Calls `TokenExchangeService.exchangeToken(token)` which delegates to registered `TokenExchangePlugin` instances
3. Stores the resulting `PrincipalContext` in the request-scoped `RequestScopePrincipalService`
4. Sets the JAX-RS `SecurityContext` on the request so the principal is available via `jakarta.ws.rs.core.SecurityContext.getUserPrincipal()`

### Accessing the Principal

The principal can be accessed in two ways:

**Via JAX-RS SecurityContext** (standard approach):

```kotlin
@GET
fun getUser(@Context securityContext: SecurityContext): Response {
    val principal = securityContext.userPrincipal as PrincipalContext
    val userId = principal.getPrincipalId()
    // ...
}
```

**Via RequestScopePrincipalService** (CDI injection):

```kotlin
@Inject
lateinit var principalService: RequestScopePrincipalService

fun doSomething() {
    val principal = principalService.ensurePrincipal()
    // ...
}
```

The SecurityContext also supports role checking:

```kotlin
securityContext.isUserInRole("backoffice.admin") // checks against global roles
securityContext.authenticationScheme              // returns "Bearer"
```

## Core Concepts

### PrincipalContext

`PrincipalContext` extends `java.security.Principal` and carries the authenticated user's identity:

```kotlin
interface PrincipalContext : Principal {
    fun getPrincipalId(): UUID
    fun getGlobalRoles(): List<String>
    fun getEntityRoles(): List<EntityRole>
}
```

The default implementation is `DefaultPrincipalContext`. Libraries like `platform-core-lib` can extend this with richer types (e.g. `ApiPrincipal`) that carry additional token metadata while remaining compatible with the authz framework.

### Roles

A role is a named set of permissions. Roles come in two flavours:

**Global roles** grant access across all entities. Example: `backoffice.admin` can access all merchants.

**Entity roles** scope permissions to specific entity instances:

```kotlin
data class EntityRole(
    val type: String,       // e.g. "org", "partner", "merchant"
    val roles: List<String>,// e.g. ["org.admin"]
    val ids: List<String>   // e.g. ["org-123"]
)
```

Example: `org.admin` with `ids = ["org-123"]` grants admin permissions only for that specific org.

### Permissions

Permissions follow the format `<resource>:<operation>`:

- **resource** — plural noun matching REST conventions (e.g. `users`, `payees`, `sms.messages`)
- **operation** — one of `create`, `read`, `update`, `delete`, or `all` (shorthand for all four)

```yaml
- name: org.admin
  permissions:
    - payees:all    # equivalent to payees:create + payees:read + payees:update + payees:delete
```

## Access Control

### Simple Entity Permission Check

```kotlin
class CreateUserAccessControl : BaseEntityAccessControl(
    permission = Permission.of("users:create"),
    entityType = "org",
    extractEntityId = { ctx -> ctx.firstOfType(CreateUserRequest::class.java).orgId }
)
```

### Custom Access Control with Before/After Hooks

For complex scenarios, implement the `AccessControl` interface as a managed bean:

```kotlin
@ApplicationScoped
class ComplexAccessControl : AccessControl<MyResponseType> {
    @Inject
    lateinit var someService: SomeService

    override fun before(ctx: DefaultAccessControlContext) {
        // Pre-execution checks
    }

    override fun after(result: MyResponseType, ctx: DefaultAccessControlContext): MyResponseType {
        // Filter or modify the result based on permissions
        return result
    }
}
```

## Testing

The `authz-testing` module provides `MockTokenExchangeService`, a `TokenExchangePlugin` that maps fixed token strings to principals:

| Token                    | Principal                                        |
|--------------------------|--------------------------------------------------|
| `backoffice-admin-token` | Global role `backoffice.admin`                   |
| `no-roles-token`         | No roles                                         |
| `org-user-token`         | Entity role `org.user` for entity `org-1`        |

Use it in integration tests by passing these tokens as Bearer tokens in the `Authorization` header.
