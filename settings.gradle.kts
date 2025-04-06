rootProject.name = "authz-lib"

include("authz-core")
include("authz-quarkus")
include("authz-testing")
include("authz-sample-app")

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

