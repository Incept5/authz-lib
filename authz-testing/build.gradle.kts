

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

