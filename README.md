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

## How Role Mapping Works

This section explains how services that depend on authz-lib wire up roles, resolve permissions, and enforce access at request time.

### 1. YAML to Role Objects

Services define their roles under `incept5.authz.roles` in `application.yaml`. The authz-quarkus module binds this config automatically via Quarkus `@ConfigMapping`:

```yaml
incept5:
  authz:
    roles:
      - name: backoffice.admin
        permissions:
          - ".*:all"          # wildcard — matches any resource and operation
      - name: partner.user
        permissions:
          - partner:read
          - webhook:read
      - name: partner.admin
        extends-role: partner.user   # inherits all of partner.user's permissions
        permissions:
          - partner:update
          - webhook:create
      - name: merchant.user
        permissions:
          - merchant:read
      - name: merchant.admin
        extends-role: merchant.user
        permissions:
          - merchant:update
```

Each entry becomes a `Role` object with a name, a list of permission strings, and an optional `extendsRole` pointer.

### 2. Role Inheritance (`extends-role`)

When a role declares `extends-role`, `SimplePermissionService` resolves permissions recursively at startup:

- `partner.admin` extends `partner.user` → gets `partner:update`, `webhook:create` **plus** `partner:read`, `webhook:read`
- `merchant.admin` extends `merchant.user` → gets `merchant:update` **plus** `merchant:read`

Multi-level inheritance works too (e.g. `super_admin` → `admin` → `user`). Circular references are guarded against with a visited set.

### 3. Wildcard Permissions

Permissions use regex matching internally. A role with `".*:all"` matches **any** `resource:operation` combination:

```
Permission.of("merchant:create").matches(Permission.of(".*:all")) → true
```

This makes `backoffice.admin` a super-admin that passes every permission check.

### 4. Request-Time Flow

```
JWT Token → AuthzFilter → TokenExchangePlugin → PrincipalContext → @AuthzCheck
```

1. **AuthzFilter** (a JAX-RS `ContainerRequestFilter` at `AUTHENTICATION` priority) intercepts the request and extracts the Bearer token
2. **TokenExchangePlugin** validates the token and maps its claims to hierarchical roles. For example, a Supabase-based plugin might map:
   - `entity_admin` + `entity_type=partner` → `partner.admin`
   - `entity_user` + `entity_type=merchant` → `merchant.user`
   - `platform_admin` → `backoffice.admin`
3. The plugin returns a **PrincipalContext** containing `globalRoles` and `entityRoles` (which include entity IDs)
4. The principal is stored in the request-scoped `RequestScopePrincipalService` and the JAX-RS `SecurityContext`

### 5. Permission Enforcement

Controllers and services annotate methods with `@AuthzCheck(SomeAccessControl::class)`. Each `AccessControl` implementation calls:

```kotlin
ctx.authz().ensureOperationAllowedForPrincipal(Permission.of("webhook:read"))
```

This resolves the principal's roles → collects all permissions (including inherited ones) → regex-matches against the required permission. If no match is found, a `ForbiddenException` (403) is thrown.

### 6. Entity Scoping

Beyond permission checks, the service layer verifies entity ownership. A `partner.admin` for partner A cannot access partner B's data — the `BaseEntityAccessControl` checks that the target entity ID appears in the principal's `entityRoles[].ids`.

### 7. Public Endpoints

Implement `IgnoreAuthzFilterProvider` to whitelist paths that bypass authentication entirely:

```kotlin
@Singleton
class MyIgnoreAuthzFilterProvider : IgnoreAuthzFilterProvider {
    override fun ignoreRegexes(): List<String> = listOf(
        "/api/v1/public/.*",
        "/health.*"
    )
}
```

Matching paths skip the `AuthzFilter`, so no token is required.

## Protecting Endpoints

Access control starts at the controller level and works in layers:

1. **`@Authorized`** on the class — enables the authz interceptor for all methods
2. **`@AuthzCheck`** on each method — binds a specific `AccessControl` class that enforces permissions

### Step 1: Annotate the Controller

```kotlin
@Path("/api/v1/users")
@Authorized
class UserController {

    @Inject
    lateinit var userService: UserService

    @GET
    @Path("/{userId}")
    @AuthzCheck(ReadUserAccessControl::class)
    fun getUser(@PathParam("userId") userId: UUID): UserResponse {
        return userService.getUser(userId)
    }

    @POST
    @AuthzCheck(CreateUserAccessControl::class)
    fun createUser(request: CreateUserRequest): UserResponse {
        return userService.createUser(request)
    }
}
```

Both `@Authorized` (class-level) and `@AuthzCheck` (method-level) are required. Without `@Authorized`, the interceptor is not activated and `@AuthzCheck` has no effect.

### Step 2: Write an AccessControl Class

The `AccessControl` interface provides `before` and `after` hooks around the annotated method. Use `ctx.authz()` to access the `AuthzContext` helper methods.

**Key principles:**

- **Backoffice users** (global permissions) typically have full access — check with `principalHasGlobalPermission()`
- **Entity users** (partners, merchants) are restricted to their domain — match entity IDs from the path or result against the principal's entity IDs
- If the entity ID is **in the path** (e.g. `/partner/{partnerId}`), validate in `before()` using `extractEntityId`
- If the entity ID is **only in the result** (e.g. reading a user whose entity isn't in the URL), validate in `after()`
- If the entity ID is **not directly available**, inject a repository to look it up (e.g. find a transaction by ID, then check its partnerId/merchantId)

### Simple Case: Entity ID in the Path

When the entity ID is available from the method arguments (e.g. a path parameter or request body), use `BaseEntityAccessControl`:

```kotlin
class CreateUserAccessControl : BaseEntityAccessControl(
    permission = Permission.of("users:create"),
    entityType = "org",
    extractEntityId = { ctx -> ctx.firstOfType(CreateUserRequest::class.java).orgId }
)
```

This handles both backoffice (global permission → allow) and entity-scoped checks in a single line.

### Full Example: Entity ID Only in the Result

When the entity ID is not in the request path, enforce scoping in the `after()` hook by inspecting the result:

```kotlin
class ReadUserAccessControl : AccessControl<Any?> {

    private val permission = Permission.of("user:read")

    override fun before(ctx: DefaultAccessControlContext) {
        // Ensure the principal has the permission at all (global or entity-level)
        ctx.authz().ensureOperationAllowedForPrincipal(permission)
    }

    override fun after(result: Any?, ctx: DefaultAccessControlContext): Any? {
        if (result !is UserResponse) return result

        val targetEntityId = result.entityId ?: return result

        // Backoffice users have global permission — allow access to any user
        if (ctx.authz().principalHasGlobalPermission(permission)) {
            return result
        }

        // Partner-scoped: target user's entityId must be in the principal's allowed partner IDs
        if (ctx.authz().principalHasEntityRole("partner")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "partner")
            if (targetEntityId !in allowedIds) {
                throw AuthzException(
                    AuthzErrorCodes.PERMISSION_DENIED,
                    "User access denied: principal does not have access to user in entity $targetEntityId"
                )
            }
            return result
        }

        // Merchant-scoped: target user's entityId must be in the principal's allowed merchant IDs
        if (ctx.authz().principalHasEntityRole("merchant")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "merchant")
            if (targetEntityId !in allowedIds) {
                throw AuthzException(
                    AuthzErrorCodes.PERMISSION_DENIED,
                    "User access denied: principal does not have access to user in entity $targetEntityId"
                )
            }
            return result
        }

        // No matching entity role and no global permission — deny
        throw AuthzException(
            AuthzErrorCodes.PERMISSION_DENIED,
            "User access denied: principal has no entity scope for user in entity $targetEntityId"
        )
    }
}
```

### Repository Lookup: Entity ID Not in Path or Result

When you need to resolve the entity from another source (e.g. a transaction ID in the path), make the `AccessControl` class a CDI bean and inject a repository:

```kotlin
@ApplicationScoped
class ReadTransactionAccessControl : AccessControl<Any?> {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val permission = Permission.of("transaction:read")

    override fun before(ctx: DefaultAccessControlContext) {
        ctx.authz().ensureOperationAllowedForPrincipal(permission)

        // Backoffice can access anything
        if (ctx.authz().principalHasGlobalPermission(permission)) return

        // Look up the transaction to find which entity it belongs to
        val transactionId = ctx.firstArg<String>()
        val transaction = transactionRepository.findById(transactionId)
            ?: throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Transaction not found")

        // Check partner access
        if (ctx.authz().principalHasEntityRole("partner")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "partner")
            if (transaction.partnerId !in allowedIds) {
                throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Access denied to transaction")
            }
            return
        }

        // Check merchant access
        if (ctx.authz().principalHasEntityRole("merchant")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "merchant")
            if (transaction.merchantId !in allowedIds) {
                throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Access denied to transaction")
            }
            return
        }

        throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "No entity scope for transaction")
    }
}
```

### AuthzContext Helper Methods

The `ctx.authz()` object provides the following methods for access control logic:

| Method | Use |
|--------|-----|
| `ensureOperationAllowedForPrincipal(perm)` | Pre-check: principal has the permission globally or for any entity |
| `principalHasGlobalPermission(perm)` | Check if backoffice-level (global) access — if true, skip entity checks |
| `principalHasEntityRole(type)` | Check if the principal has any role for an entity type (e.g. "partner") |
| `specificEntityIds(perm, type)` | Get the list of entity IDs the principal can access for a permission + type |
| `ensurePrincipalHasPermission(perm, type, entityId)` | All-in-one: checks global OR entity-scoped access for a specific entity ID |
| `principalHasPermission(perm, type, entityId)` | Boolean version of the above |

## Testing

The `authz-testing` module provides `MockTokenExchangeService`, a `TokenExchangePlugin` that maps fixed token strings to principals:

| Token                    | Principal                                        |
|--------------------------|--------------------------------------------------|
| `backoffice-admin-token` | Global role `backoffice.admin`                   |
| `no-roles-token`         | No roles                                         |
| `org-user-token`         | Entity role `org.user` for entity `org-1`        |

Use it in integration tests by passing these tokens as Bearer tokens in the `Authorization` header.
