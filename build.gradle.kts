plugins {
    `kotlin-dsl`
    `maven-publish`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

dependencies {
    "implementation"(libs.gson)
    "api"(libs.loom)
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
}

java {
    withSourcesJar()
    withJavadocJar()
}

gradlePlugin {
    website = "https://github.com/stracciatella-client/stracciatella-gradle"
    vcsUrl = "https://github.com/stracciatella-client/stracciatella-gradle"
    plugins {
        register("stracciatella") {
            description = project.description
            id = "net.stracciatella.stracciatella"
            displayName = "Stracciatella Gradle Plugin"
            implementationClass = "net.stracciatella.gradle.plugin.StracciatellaPlugin"
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