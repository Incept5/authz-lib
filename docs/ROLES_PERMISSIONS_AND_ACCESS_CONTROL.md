# Roles, Permissions and Access Control

## Overview

The authz framework provides annotation-driven access control for Quarkus services. You define roles and permissions in YAML, protect methods with `@AuthzCheck`, and the framework handles token exchange, principal resolution, and permission enforcement.

This document explains the core model (roles, permissions, principals), how `AccessControl` classes work, and best practices for writing them concisely.

---

## Permissions

A permission is a `resource:operation` pair. The `Permission.of()` factory parses this format:

```kotlin
val permission = Permission.of("partner:read")
// → Permission(resource = "partner", operation = AccessOperation.READ)
```

### Operations

| Operation | Meaning |
|-----------|---------|
| `create`  | Create a resource |
| `read`    | Read a resource |
| `update`  | Update a resource |
| `delete`  | Delete a resource |
| `all`     | Shorthand for all four operations |

### Matching Rules

- **Exact match:** `partner:read` matches `partner:read`
- **Operation wildcard:** `partner:all` matches `partner:read`, `partner:create`, etc.
- **Resource wildcard:** `.*:all` matches any `resource:operation` combination (super-admin)

```kotlin
Permission.of("partner:all").matches(Permission.of("partner:read"))  // true
Permission.of(".*:all").matches(Permission.of("merchant:create"))     // true
Permission.of("partner:read").matches(Permission.of("partner:update")) // false
```

---

## Roles

A role is a named collection of permissions defined in `application.yaml`:

```yaml
incept5:
  authz:
    roles:
      - name: backoffice.admin
        permissions:
          - ".*:all"               # matches everything

      - name: partner.user
        permissions:
          - partner:read
          - webhook:read

      - name: partner.admin
        extends-role: partner.user  # inherits partner.user's permissions
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

### Role Inheritance

When a role declares `extends-role`, `SimplePermissionService` resolves permissions recursively at startup:

- `partner.admin` gets its own permissions (`partner:update`, `webhook:create`) **plus** all of `partner.user`'s (`partner:read`, `webhook:read`)
- Multi-level inheritance works (e.g. `super_admin` -> `admin` -> `user`)
- Circular references are guarded with a visited set

### Two Flavours of Roles

**Global roles** grant access across all entities. A `backoffice.admin` with `.*:all` can access any partner, merchant, or resource without restriction.

**Entity roles** scope permissions to specific entity instances:

```kotlin
data class EntityRole(
    val type: String,        // e.g. "partner", "merchant", "org"
    val roles: List<String>, // e.g. ["partner.admin"]
    val ids: List<String>    // e.g. ["partner-123", "partner-456"]
)
```

A user with `EntityRole(type="partner", roles=["partner.admin"], ids=["partner-123"])` has `partner.admin` permissions **only** for `partner-123`.

---

## PrincipalContext

The authenticated user is represented by `PrincipalContext`:

```kotlin
interface PrincipalContext : Principal {
    fun getPrincipalId(): UUID
    fun getGlobalRoles(): List<String>
    fun getEntityRoles(): List<EntityRole>
}
```

The `platform-core-lib` extends this with `ApiPrincipal`, which carries additional JWT metadata (subject, scopes, client ID, entity type).

---

## The AccessControl Interface

`AccessControl<R>` defines two hooks that wrap an annotated method:

```kotlin
interface AccessControl<R> {
    fun before(ctx: DefaultAccessControlContext) {}
    fun after(result: R?, ctx: DefaultAccessControlContext): R? = result
}
```

| Hook | When it runs | Typical use |
|------|-------------|-------------|
| `before()` | Before the method executes | Check permissions, reject early |
| `after()` | After the method returns | Filter or validate the result |

### DefaultAccessControlContext

The context gives you access to the method's arguments and the authorization system:

```kotlin
ctx.args()                // Array of method arguments
ctx.firstArg<String>()    // First argument cast to String
ctx.secondArg<UUID>()     // Second argument cast to UUID
ctx.firstOfType(CreateUserRequest::class.java)  // First arg matching a type
ctx.authz()               // AuthzContext for permission checks
```

### Wiring: @Authorized + @AuthzCheck

```kotlin
@Path("/api/v1/partner")
@Authorized                                          // 1. Activates the interceptor on the class
class PartnerController {

    @GET
    @Path("/{partnerId}")
    @AuthzCheck(PartnerResourceAccessControl::class)  // 2. Binds this AccessControl to the method
    fun getPartner(@PathParam("partnerId") partnerId: String): PartnerResponse {
        // ...
    }
}
```

Both annotations are required. `@Authorized` (class-level) activates the `AuthzCheckMethodInterceptor`. `@AuthzCheck` (method-level) specifies which `AccessControl` implementation to run.

---

## AuthzContext Helper Methods

The `ctx.authz()` object (backed by `DelegatingAuthzService`) provides these methods:

| Method | Returns | Use case |
|--------|---------|----------|
| `ensurePrincipalHasPermission(perm, type, entityId)` | `void` (throws 403) | **All-in-one check**: global OR entity-scoped access for a specific entity ID |
| `principalHasPermission(perm, type, entityId)` | `Boolean` | Boolean version of the above |
| `ensureOperationAllowedForPrincipal(perm)` | `void` (throws 403) | Pre-check: principal has permission globally or for *any* entity (before you know the entity ID) |
| `principalHasGlobalPermission(perm)` | `Boolean` | Check if principal has backoffice/admin-level global access |
| `ensurePrincipalHasGlobalPermission(perm)` | `void` (throws 403) | Throwing version of the above |
| `principalHasEntityRole(type)` | `Boolean` | Check if principal has any entity role of a given type (e.g. "partner") |
| `specificEntityIds(perm, type)` | `List<String>` | Get the entity IDs the principal can access for a permission + entity type |
| `ensurePrincipal()` | `PrincipalContext` | Get the principal or throw if not authenticated |
| `principalCanAssignRole(roleName)` | `Boolean` | Check if the principal can assign a role to another user |

### How `ensurePrincipalHasPermission` Works Internally

This is the most commonly used method. It performs a **two-stage check**:

1. **Global check:** Resolves all permissions for the principal's global roles. If any match the required permission, access is granted immediately.
2. **Entity-scoped check** (only if global fails): Finds an `EntityRole` where the `type` matches AND the `ids` contain the requested entity ID. Resolves permissions for that entity role's roles. If any match, access is granted.

If neither stage matches, throws `AuthzException` (403 Forbidden).

This means a single call handles both admin users (global access) and scoped users (entity access).

---

## How It All Fits Together: Sequence Diagram

The following diagram traces a request through the full authorization pipeline:

```mermaid
sequenceDiagram
    participant Client
    participant AuthzFilter
    participant TokenExchangePlugin as SupabaseTokenExchangePlugin
    participant PrincipalService as RequestScopePrincipalService
    participant Interceptor as AuthzCheckMethodInterceptor
    participant AccessControl as PartnerResourceAccessControl
    participant AuthzContext as DelegatingAuthzService
    participant PermissionService as SimplePermissionService
    participant Controller as PartnerController

    Client->>AuthzFilter: GET /api/v1/partner/{partnerId}<br/>Authorization: Bearer <token>

    Note over AuthzFilter: Priority: AUTHENTICATION

    AuthzFilter->>TokenExchangePlugin: exchangeToken(token)
    TokenExchangePlugin->>TokenExchangePlugin: Validate JWT (DualJwtValidator)
    TokenExchangePlugin->>TokenExchangePlugin: Map JWT roles to globalRoles + entityRoles
    TokenExchangePlugin-->>AuthzFilter: ApiPrincipal<br/>(globalRoles, entityRoles)
    AuthzFilter->>PrincipalService: setPrincipal(apiPrincipal)

    Client->>Interceptor: Invoke getPartner(partnerId)

    Note over Interceptor: @Authorized class + @AuthzCheck method

    Interceptor->>Interceptor: Read @AuthzCheck annotation<br/>resolve PartnerResourceAccessControl
    Interceptor->>Interceptor: Build DefaultAccessControlContext<br/>(args=[partnerId], authzContext)

    Interceptor->>AccessControl: before(ctx)

    AccessControl->>AccessControl: partnerId = ctx.firstArg<String>()
    AccessControl->>AccessControl: permission = Permission.of("partner:read")

    AccessControl->>AuthzContext: ensurePrincipalHasPermission<br/>(permission, "partner", partnerId)

    AuthzContext->>PrincipalService: ensurePrincipal()
    PrincipalService-->>AuthzContext: ApiPrincipal

    AuthzContext->>PermissionService: findMatchingPermission<br/>(permission, "partner", [partnerId], principal)

    Note over PermissionService: Stage 1: Global Permission Check

    PermissionService->>PermissionService: Get principal.getGlobalRoles()
    PermissionService->>PermissionService: Resolve roles to permissions<br/>(with inheritance)
    PermissionService->>PermissionService: Any permission matches<br/>"partner:read" or ".*:all"?

    alt Global permission found
        PermissionService-->>AuthzContext: Matched global permission
        Note over PermissionService: Admin/backoffice user — ALLOW
    else No global permission
        Note over PermissionService: Stage 2: Entity-Scoped Check

        PermissionService->>PermissionService: Get principal.getEntityRoles()
        PermissionService->>PermissionService: Find EntityRole where<br/>type == "partner" AND<br/>ids contains partnerId

        alt Entity role found with matching ID
            PermissionService->>PermissionService: Resolve entity role to permissions
            PermissionService->>PermissionService: Any permission matches "partner:read"?

            alt Permission matches
                PermissionService-->>AuthzContext: Matched entity permission
                Note over PermissionService: Scoped user — ALLOW
            else No matching permission
                PermissionService-->>AuthzContext: No match
                AuthzContext-->>AccessControl: throw AuthzException (403)
                AccessControl-->>Client: 403 Forbidden
            end
        else No matching entity role/ID
            PermissionService-->>AuthzContext: No match
            AuthzContext-->>AccessControl: throw AuthzException (403)
            AccessControl-->>Client: 403 Forbidden
        end
    end

    AuthzContext-->>AccessControl: Permission granted

    AccessControl-->>Interceptor: before() completed

    Interceptor->>Controller: getPartner(partnerId)
    Controller-->>Interceptor: PartnerResponse

    Interceptor->>AccessControl: after(result, ctx)
    AccessControl-->>Interceptor: result (pass-through)

    Interceptor-->>Client: 200 OK + PartnerResponse
```

---

## Best Practices and Examples

### Pattern 1: Entity ID in the Path — Use BaseEntityAccessControl

When the entity ID is available from method arguments, use `BaseEntityAccessControl` for maximum conciseness. It calls `ensurePrincipalHasPermission` under the hood, handling both global and entity-scoped access in one line:

```kotlin
class PartnerResourceAccessControl : BaseEntityAccessControl(
    permission = Permission.of("partner:read"),
    entityType = "partner",
    extractEntityId = { ctx -> ctx.firstArg() }
)
```

Extracting from a request body:

```kotlin
class CreateUserAccessControl : BaseEntityAccessControl(
    permission = Permission.of("users:create"),
    entityType = "org",
    extractEntityId = { ctx -> ctx.firstOfType(CreateUserRequest::class.java).orgId }
)
```

### Pattern 2: Entity ID in the Path — Custom AccessControl (One-Liner)

When you need a custom `AccessControl` class but the entity ID is still in the arguments, use `ensurePrincipalHasPermission` directly. This checks global OR entity-scoped access in a single call:

```kotlin
class PartnerResourceAccessControl : AccessControl<Any?> {

    private val permission = Permission.of("partner:read")

    override fun before(ctx: DefaultAccessControlContext) {
        val partnerId = ctx.firstArg<String>()
        // Checks global OR entity-scoped access in one call
        ctx.authz().ensurePrincipalHasPermission(permission, "partner", partnerId)
    }

    override fun after(result: Any?, ctx: DefaultAccessControlContext): Any? = result
}
```

### Pattern 3: Entity ID Only in the Result — Use after()

When the entity ID is not available until the method returns (e.g. reading a user by user ID, where the entity affiliation is on the result):

```kotlin
class ReadUserAccessControl : AccessControl<Any?> {

    private val permission = Permission.of("user:read")

    override fun before(ctx: DefaultAccessControlContext) {
        // Pre-check: can the principal do this operation at all?
        ctx.authz().ensureOperationAllowedForPrincipal(permission)
    }

    override fun after(result: Any?, ctx: DefaultAccessControlContext): Any? {
        if (result !is UserResponse) return result
        val entityId = result.entityId ?: return result

        // Global permission = unrestricted access
        if (ctx.authz().principalHasGlobalPermission(permission)) return result

        // Entity-scoped: check the result's entity is in the principal's allowed set
        if (ctx.authz().principalHasEntityRole("partner")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "partner")
            if (entityId !in allowedIds) {
                throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED,
                    "Access denied: principal cannot access entity $entityId")
            }
            return result
        }

        if (ctx.authz().principalHasEntityRole("merchant")) {
            val allowedIds = ctx.authz().specificEntityIds(permission, "merchant")
            if (entityId !in allowedIds) {
                throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED,
                    "Access denied: principal cannot access entity $entityId")
            }
            return result
        }

        throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED,
            "No entity scope for user in entity $entityId")
    }
}
```

### Pattern 4: Repository Lookup — Entity ID Not in Path or Result

When you need to resolve the owning entity from a related resource (e.g. checking partner access for a transaction), make the `AccessControl` a CDI bean and inject a repository:

```kotlin
@ApplicationScoped
class ReadTransactionAccessControl : AccessControl<Any?> {

    @Inject
    lateinit var transactionRepository: TransactionRepository

    private val permission = Permission.of("transaction:read")

    override fun before(ctx: DefaultAccessControlContext) {
        val transactionId = ctx.firstArg<String>()

        // Global access = skip entity check
        if (ctx.authz().principalHasGlobalPermission(permission)) return

        // Look up the owning entity
        val transaction = transactionRepository.findById(transactionId)
            ?: throw AuthzException(AuthzErrorCodes.PERMISSION_DENIED, "Transaction not found")

        // Check entity-scoped access
        ctx.authz().ensurePrincipalHasPermission(permission, "partner", transaction.partnerId)
    }
}
```

### Pattern 5: Global-Only Permission

For operations that should only be available to backoffice users:

```kotlin
class SystemConfigAccessControl : AccessControl<Any?> {

    private val permission = Permission.of("system-config:update")

    override fun before(ctx: DefaultAccessControlContext) {
        ctx.authz().ensurePrincipalHasGlobalPermission(permission)
    }

    override fun after(result: Any?, ctx: DefaultAccessControlContext): Any? = result
}
```

### Pattern 6: List Filtering with specificEntityIds

For list endpoints where global users see everything and entity users see only their scoped data:

```kotlin
class ListPartnersAccessControl : AccessControl<Any?> {

    private val permission = Permission.of("partner:read")

    override fun before(ctx: DefaultAccessControlContext) {
        ctx.authz().ensureOperationAllowedForPrincipal(permission)
    }

    override fun after(result: Any?, ctx: DefaultAccessControlContext): Any? {
        if (result !is List<*>) return result

        // Global users see everything
        if (ctx.authz().principalHasGlobalPermission(permission)) return result

        // Entity users see only their allowed partners
        val allowedIds = ctx.authz().specificEntityIds(permission, "partner")
        return (result as List<PartnerResponse>).filter { it.id in allowedIds }
    }
}
```

---

## Decision Guide: Which Pattern to Use

```
Is the entity ID available from method arguments (path param, request body)?
├── YES → Is a one-liner sufficient?
│   ├── YES → Pattern 1: BaseEntityAccessControl
│   └── NO  → Pattern 2: Custom AccessControl with ensurePrincipalHasPermission
├── NO  → Is the entity ID in the result?
│   ├── YES → Pattern 3: Use after() to validate the result
│   └── NO  → Pattern 4: Inject a repository and look it up in before()

Is this a global-only operation (no entity scoping)?
└── YES → Pattern 5: ensurePrincipalHasGlobalPermission

Is this a list endpoint needing result filtering?
└── YES → Pattern 6: specificEntityIds in after()
```

---

## Testing

The `authz-testing` module provides `MockTokenExchangeService` with pre-configured tokens:

| Token | Principal |
|-------|-----------|
| `backoffice-admin-token` | Global role `backoffice.admin` |
| `no-roles-token` | No roles |
| `org-user-token` | Entity role `org.user` for entity `org-1` |

Use these as Bearer tokens in integration tests:

```kotlin
given()
    .header("Authorization", "Bearer backoffice-admin-token")
    .`when`()
    .get("/api/v1/partner/partner-123")
    .then()
    .statusCode(200)
```

---

## Key Classes Reference

| Class | Module | Purpose |
|-------|--------|---------|
| `Permission` | authz-core | `resource:operation` model with matching logic |
| `AccessOperation` | authz-core | Enum: `CREATE`, `READ`, `UPDATE`, `DELETE`, `ALL` |
| `AccessControl<R>` | authz-core | Interface with `before()`/`after()` hooks |
| `BaseEntityAccessControl` | authz-core | Helper for the common entity-scoped check pattern |
| `DefaultAccessControlContext` | authz-core | Context with `args()`, `firstArg()`, `authz()` |
| `AuthzContext` | authz-core | Interface for all permission check methods |
| `DelegatingAuthzService` | authz-core | Default `AuthzContext` implementation |
| `SimplePermissionService` | authz-core | Resolves role -> permission mappings with inheritance |
| `PrincipalContext` | authz-core | Authenticated user: ID, global roles, entity roles |
| `EntityRole` | authz-core | Scoped role: type, roles, entity IDs |
| `AuthzFilter` | authz-quarkus | JAX-RS filter: token -> principal |
| `AuthzCheckMethodInterceptor` | authz-quarkus | Jakarta interceptor: runs `AccessControl` before/after |
| `@Authorized` | authz-quarkus | Class-level annotation to activate the interceptor |
| `@AuthzCheck` | authz-core | Method-level annotation binding an `AccessControl` class |
| `ApiPrincipal` | platform-core-lib | Extended `PrincipalContext` with JWT/OAuth metadata |
| `SupabaseTokenExchangePlugin` | platform-core-lib | Token validation and role mapping |
