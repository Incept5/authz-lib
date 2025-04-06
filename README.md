# Core Authz

## Overview

The base core-authz module provides models and framework agnostic abstractions and components for 
implementing annotation driven access control checks at the controller or service layer.

## Installation

### Gradle (Kotlin DSL)

Add the JitPack repository to your build file:

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

Add the dependencies:

```kotlin
// For core functionality only
implementation("com.github.incept5.authz-lib:authz-core:latest.release")

// For Quarkus integration
implementation("com.github.incept5.authz-lib:authz-quarkus:latest.release")

// For testing utilities
testImplementation("com.github.incept5.authz-lib:authz-testing:latest.release")
```

### Gradle (Groovy DSL)

Add the JitPack repository:

```groovy
repositories {
    mavenCentral()
    maven { url 'https://jitpack.io' }
}
```

Add the dependencies:

```groovy
// For core functionality only
implementation 'com.github.incept5.authz-lib:authz-core:latest.release'

// For Quarkus integration
implementation 'com.github.incept5.authz-lib:authz-quarkus:latest.release'

// For testing utilities
testImplementation 'com.github.incept5.authz-lib:authz-testing:latest.release'
```

### Maven

Add the JitPack repository to your pom.xml:

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```

Add the dependencies:

```xml
<!-- For core functionality only -->
<dependency>
    <groupId>com.github.incept5.authz-lib</groupId>
    <artifactId>authz-core</artifactId>
    <version>latest.release</version>
</dependency>

<!-- For Quarkus integration -->
<dependency>
    <groupId>com.github.incept5.authz-lib</groupId>
    <artifactId>authz-quarkus</artifactId>
    <version>latest.release</version>
</dependency>

<!-- For testing utilities -->
<dependency>
    <groupId>com.github.incept5.authz-lib</groupId>
    <artifactId>authz-testing</artifactId>
    <version>latest.release</version>
    <scope>test</scope>
</dependency>
```

## Usage

Add the authz-quarkus dependency to your project and then you need to add an `@Authorized` annotation to your
business layer service to mark it for access control checks and then add an `@AuthzCheck` annotation to each method
that needs to be protected like so:

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


## Configuration

### Quarkus Configuration

To configure the authorization system in your Quarkus application, add the following to your `application.yaml` file:

```yaml
authz:
  # Define your roles and their permissions
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
  
  # Optional: Configure token exchange service
  token-exchange:
    enabled: true
    plugins:
      - my-custom-token-exchange-plugin
```

### Custom Roles Provider

You can implement a custom `RolesProvider` to define your application's roles programmatically:

```kotlin
@Singleton
class ExampleRolesProvider : RolesProvider {
    override fun getRoles(): List<Role> {
        return listOf(
            Role(
                name = "backoffice.admin",
                permissions = setOf(
                    Permission.of("users:all"),
                    Permission.of("orgs:all")
                )
            ),
            Role(
                name = "org.admin",
                permissions = setOf(
                    Permission.of("users:read"),
                    Permission.of("users:create"),
                    Permission.of("users:update")
                )
            )
        )
    }
}
```

## Roles

A Role is just a label but is mapped to a set of permissions.. so in essence it becomes a short-hand for a set of permissions.

An example of a role might be:

* **backoffice.admin**
  * global admin with full access
* **org.admin**
  * admin for a specific organisation
* **org.support**
  * support for a specific organisation - read only access

One role can extend another role and will therefore inherit permissions from it

Roles come in 2 different flavours..

### Global Roles

Global roles are not associated with specific entities so a user with a global role will have some access to 
all entites referred to by the associated permissions.

An example of a global role is backoffice.admin which would give the user the ability to access 
ALL merchants, for example, as well as add new ones.

No specific resources are associated with these roles, hence the name.

### Entity Roles

Entity roles are the glue that bring together a set of permissions (associated with the role), a specific user, and a set of resources that are associated with a major entity.

Each entity role consists of:

* **type**
  * the type of entity associated with the user (e.g. org)

* **role**
  * the label associated with the set of permissions that are being bestowed upon the user

* **entityIds**
  * the list of 1 or more uuid entity ids that link to a set of specific entities 
  * usually this is just the primary key of a single entity (e.g. orgId)
  * it does allow for extension in the future (e.g. payor hierarchy)

An example of an entity role is org.admin which links a user to a specific Org entity (via orgId) and bestows 
permissions for interacting with just that specific Org and not all Orgs.

### Role Map

We inject the mapping between role name and permissions via configuration in the service yaml file:

    authz:
      roles:
        - name: backoffice.admin
          permissions:
            - users:create


## Permissions

A permission combines some resource indicator with some access control logic and will appear in the Resource Server codebase (in code or annotations) in order to protect that resource from being accessed by a user that should not be able to.

A permission is of the following form:

    <resource>:<access-operation>

### Resource

Generally the resource is either a top level resource entity (e.g. payees,payors,payouts or payments) or a sub-resource (e.g. payees.paymentChannels). We always use plurals for consistency (the same as REST endpoints).

Some resources only relate to a particular service or domain and so might have a prefix for clarity (e.g. sms.messages:create)
Access Operation

Access Operation follows the standard form of CRUD so: 
* create
* read
* update
* delete

NOTE: there is no list operation as that is just another form of read

In addition, there is a special operation called all which is shorthand for create+read+update+delete

all is never used in the code but it does allow us to “roll up” all 4 permissions into a single foo:all permission in the role map to keep it smaller and simpler. So instead of this:
    
    - name: velo.payor.admin
      permissions:
        - payees:create
        - payees:read
        - payees:update
        - payees:delete

We just need this:

    - name: velo.payor.admin
      permissions:
        - payees:all

## Example Code

### Add an @AuthzCheck to a Use Case method

Each Use Case method should have an @AuthzCheck annotation that specifies Access Control class to use for authorization.

    @AuthzCheck(CreateUserAccessControl::class)
    override fun createNewUser(request: CreateUserRequest): User {
        // check that the org exists
        fetchOrgService.ensureOrgById(request.orgId)
        ...
    }

### Access Control class defines the logic

#### Simple Entity Permission Check
Can be a simple permission check for a particular entity id:

```kotlin
// ensure the user can create users within the target org
class CreateUserAccessControl : BaseEntityAccessControl(
    permission = Permission.of("users:create"),
    entityType = "org",
    extractEntityId = { ctx -> ctx.firstOfType(CreateUserRequest::class.java).orgId }
)
```

In this case we are using the BaseEntityAccessControl base class which expects us to define the permission, the entity type 
and a function to extract the entity id from the request context.

#### Complex Managed Bean Logic
Or we can have a more complex example that is a managed bean and uses collaborators to perform logic both before the method is
run and after the result is available by having our managed bean implement:

```kotlin
interface AccessControl<R> {
    fun before(ctx: DefaultAccessControlContext) {}
    fun after(result: R, ctx: DefaultAccessControlContext): R = result
}
```

## Advanced Usage

### Custom Token Exchange Service

You can implement a custom token exchange service by implementing the `TokenExchangePlugin` interface:

```kotlin
@Singleton
class CustomTokenExchangePlugin : TokenExchangePlugin {
    override fun getName(): String = "my-custom-token-exchange-plugin"
    
    override fun exchange(token: String): SystemPrincipal? {
        // Implement your token validation and exchange logic here
        // Return a SystemPrincipal if the token is valid, or null if not
    }
}
```

Then register your plugin in the configuration:

```yaml
authz:
  token-exchange:
    enabled: true
    plugins:
      - my-custom-token-exchange-plugin
```

### Custom Access Control Logic

For more complex authorization scenarios, you can implement a custom `AccessControl` class:

```kotlin
@ApplicationScoped
class ComplexAccessControl : AccessControl<MyResponseType> {
    @Inject
    lateinit var someService: SomeService
    
    override fun before(ctx: DefaultAccessControlContext) {
        // Perform pre-execution checks
        val request = ctx.firstOfType(MyRequestType::class.java)
        
        // Check if the user has the required permission
        ctx.requirePermission(Permission.of("resource:operation"))
        
        // Check if the user has access to the specific entity
        ctx.requireEntityPermission(
            permission = Permission.of("resource:operation"),
            entityType = "org",
            entityId = request.orgId
        )
        
        // Perform additional custom checks
        if (!someService.isValid(request)) {
            throw ForbiddenException("Invalid request")
        }
    }
    
    override fun after(result: MyResponseType, ctx: DefaultAccessControlContext): MyResponseType {
        // Optionally modify or filter the result based on permissions
        return result
    }
}
```

## Best Practices

1. **Granular Permissions**: Define permissions at a granular level to allow for fine-grained access control.

2. **Entity-Based Access Control**: Use entity roles to restrict access to specific resources.

3. **Role Hierarchy**: Design your roles with inheritance in mind to avoid permission duplication.

4. **Testing**: Always write tests for your access control logic using the provided testing utilities.

5. **Documentation**: Document your permissions and roles to make it easier for developers to understand the access control system.

6. **Consistent Naming**: Use consistent naming conventions for permissions, following the `resource:operation` pattern.

7. **Error Handling**: Provide clear error messages when access is denied to help with debugging.

## Testing

The `authz-testing` module provides utilities to help with testing secured services.

### Using MockTokenExchangeService

For unit tests, you can use the `MockTokenExchangeService` to simulate authentication:

```kotlin
@QuarkusTest
class SecureServiceTest {

    @Inject
    lateinit var secureService: ExampleSecureService
    
    @Inject
    lateinit var mockTokenExchangeService: MockTokenExchangeService
    
    @BeforeEach
    fun setup() {
        // Set up a test principal with specific permissions
        mockTokenExchangeService.setupTestPrincipal(
            SystemPrincipal(
                id = "test-user",
                name = "Test User",
                email = "test@example.com",
                permissions = setOf(Permission.of("example:read")),
                entityRoles = listOf(
                    EntityRole(
                        type = "org",
                        role = "admin",
                        entityIds = listOf("org-123")
                    )
                )
            )
        )
    }
    
    @Test
    fun `test authorized method`() {
        // This should succeed because the test principal has the required permission
        val result = secureService.authorizedMethod("org-123")
        assertEquals("Authorized method called with id: org-123", result)
    }
    
    @Test
    fun `test unauthorized method`() {
        // This should fail because the test principal doesn't have access to this org
        assertThrows<ForbiddenException> {
            secureService.authorizedMethod("org-456")
        }
    }
}