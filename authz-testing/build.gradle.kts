


plugins {
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)

    `java-library`

    // include quarkus metadata in the jar
    alias(libs.plugins.jandex)

    // publish jars to nexus
    `maven-publish`
}

dependencies {

    api(project(":authz-core"))
    api("jakarta.inject:jakarta.inject-api")

    implementation(platform(libs.quarkus.bom))
    implementation("io.quarkus:quarkus-kotlin")

    runtimeOnly(libs.kotlin.logging)
    runtimeOnly("io.quarkus:quarkus-arc")
    runtimeOnly("io.quarkus:quarkus-rest-jackson")

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            val publishGroupId = rootProject.properties["publishGroupId"]?.toString()
                ?: if (System.getenv("JITPACK") != null) {
                    "com.github.incept5.authz-lib"
                } else {
                    "com.github.incept5"
                }

            groupId = publishGroupId
            artifactId = "authz-testing"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Authz Testing")
                description.set("Testing utilities for authz-lib")
                url.set("https://github.com/incept5/authz-lib")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("incept5")
                        name.set("Incept5")
                        email.set("info@incept5.com")
                    }
                }

                scm {
                    connection.set("scm:git:github.com/incept5/authz-lib.git")
                    developerConnection.set("scm:git:ssh://github.com/incept5/authz-lib.git")
                    url.set("https://github.com/incept5/authz-lib/tree/main")
                }
            }
        }
    }
}

// For JitPack compatibility
tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

// Always publish to local Maven repository after build for local development
tasks.named("build") {
    finalizedBy(tasks.named("publishToMavenLocal"))
}
