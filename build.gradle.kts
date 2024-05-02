plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

description = "A gradle plugin to easily work with stracciatella modules"
group = "net.stracciatella.gradle.plugin"
version = "0.2.37"

dependencies {
    implementation(libs.gson)
    "api"(libs.loom)
}

repositories {
    gradlePluginPortal()
    maven("https://nexus.darkcube.eu/repository/stracciatella") { name = "Stracciatella" }
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin.jvmToolchain(8)

gradlePlugin {
    website = "https://github.com/stracciatella-client/stracciatella-gradle"
    vcsUrl = "https://github.com/stracciatella-client/stracciatella-gradle"
    plugins {
        register("stracciatella") {
            tags.add("stracciatella")
            description = project.description
            id = "net.stracciatella.stracciatella"
            displayName = "Stracciatella Gradle Plugin for modules"
            implementationClass = "net.stracciatella.gradle.plugin.StracciatellaPlugin"
        }
        register("stracciatella-base") {
            tags.add("stracciatella")
            description = project.description
            id = "net.stracciatella.stracciatella.base"
            displayName = "Stracciatella Gradle Plugin for default configuration"
            implementationClass = "net.stracciatella.gradle.plugin.StracciatellaBasePlugin"
        }
        register("stracciatella-fabric") {
            tags.add("stracciatella")
            description = project.description
            id = "net.stracciatella.stracciatella.fabric"
            displayName = "Stracciatella Gradle Plugin for use with fabric"
            implementationClass = "net.stracciatella.gradle.plugin.StracciatellaFabricPlugin"
        }
    }
}

publishing {
    repositories {
        maven("https://nexus.darkcube.eu/repository/dasbabypixel/") {
            credentials(PasswordCredentials::class)
            name = "DasBabyPixel"
        }
    }
}