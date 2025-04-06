plugins {
    // Apply the java-library plugin for API and implementation separation.
    `java-library`
    // publish to nexus
    `maven-publish`
    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(libs.incept5.error.core)

    implementation(libs.kotlin.logging)

    // unit testing
    testImplementation(libs.kotest.assertions.core.jvm)
    testImplementation(libs.kotest.framework.api)
    testImplementation(libs.mockk.dsl)
    testImplementation(libs.mockk.jvm)
    testRuntimeOnly(libs.kotest.junit5.jvm)
    testImplementation(platform(libs.kotest.bom))
    testImplementation("io.kotest:kotest-assertions-shared")
    testImplementation("io.kotest:kotest-common")

    testRuntimeOnly(libs.slf4j.simple) // slf4j backend
}


tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            // For JitPack compatibility, we need to use the correct group ID format
            // JitPack expects: com.github.{username}.{repository}
            val publishGroupId = rootProject.properties["publishGroupId"]?.toString()
                ?: if (System.getenv("JITPACK") != null) {
                    // When building on JitPack
                    "com.github.incept5.authz-lib"
                } else {
                    // For local development
                    "com.github.incept5"
                }

            // Explicitly set the coordinates
            groupId = publishGroupId
            artifactId = "authz-core"
            version = project.version.toString()

            from(components["java"])

            // POM information
            pom {
                name.set("Authz Core")
                description.set("Core functionality for Authorzation in Rest Services")
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

                // Important for JitPack to resolve dependencies correctly
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

