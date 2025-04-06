plugins {
    `java-library`
    `maven-publish`

    // Apply the org.jetbrains.kotlin.jvm Plugin to add support for Kotlin.
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    // include quarkus metadata in the jar
    alias(libs.plugins.jandex)
}

dependencies {

    api(project(":authz-core"))
    api("io.smallrye.config:smallrye-config-core")
    api("jakarta.annotation:jakarta.annotation-api")
    api("jakarta.enterprise:jakarta.enterprise.cdi-api")
    api("jakarta.inject:jakarta.inject-api")
    api("jakarta.interceptor:jakarta.interceptor-api")
    api("jakarta.ws.rs:jakarta.ws.rs-api")

    implementation(libs.kotlin.logging)
    implementation(platform(libs.quarkus.bom))
    implementation("io.quarkus:quarkus-core")
    implementation("io.quarkus:quarkus-kotlin")
    implementation("io.quarkus.arc:arc")
    implementation("org.eclipse.microprofile.config:microprofile-config-api")

    runtimeOnly(libs.incept5.error.quarkus)
    runtimeOnly("io.quarkus:quarkus-arc")
    // Use only quarkus-rest and quarkus-rest-jackson from the same version
    runtimeOnly("io.quarkus:quarkus-rest")
    runtimeOnly("io.quarkus:quarkus-rest-jackson")
    
    // Exclude any resteasy-reactive dependencies that might be pulled in transitively
    configurations.all {
        exclude(group = "io.quarkus", module = "quarkus-resteasy-reactive")
        exclude(group = "io.quarkus", module = "quarkus-resteasy-reactive-jackson")
        exclude(group = "io.quarkus", module = "quarkus-resteasy-reactive-kotlin")
    }

    // Test dependencies
    testImplementation("io.quarkus:quarkus-junit5")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.0.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
    testImplementation("org.jetbrains.kotlin:kotlin-reflect")
    testImplementation(project(":authz-testing"))
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
            artifactId = "authz-quarkus"
            version = project.version.toString()

            // This will include all dependencies marked as 'api' as transitive dependencies
            // The 'api' configuration automatically makes dependencies transitive
            from(components["java"])

            // POM information
            pom {
                name.set("Error Quarkus")
                description.set("Quarkus integration for Authorization in Rest Services")
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

tasks.test {
    dependsOn(tasks.jandex)
    useJUnitPlatform()
}

// For JitPack compatibility
tasks.register("install") {
    dependsOn(tasks.named("publishToMavenLocal"))
}

// Always publish to local Maven repository after build for local development
tasks.named("build") {
    finalizedBy(tasks.named("publishToMavenLocal"))
}

