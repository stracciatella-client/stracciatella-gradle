package net.stracciatella.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.plugins.quality.CheckstyleExtension
import org.gradle.api.plugins.quality.CheckstylePlugin
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType

class StracciatellaBasePlugin : Plugin<Project> {
    companion object {
        const val CHECKSTYLE_URL = "https://github.com/stracciatella-client/data/raw/master/checkstyle.xml"
    }

    override fun apply(project: Project) {
        project.run {
            plugins.run {
                apply<JavaLibraryPlugin>()
                apply<CheckstylePlugin>()
            }
            val javaExtension = extensions.getByType<JavaPluginExtension>().apply {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(21))
                    vendor.set(JvmVendorSpec.ADOPTIUM)
                }
            }
            extensions.getByType<CheckstyleExtension>().run {
                maxErrors = 0
                maxWarnings = 0
                config = resources.text.fromUri(uri(CHECKSTYLE_URL))
            }
            pluginManager.withPlugin("java") {
                val toolchainService = extensions.getByType<JavaToolchainService>()
                tasks.withType<JavaCompile>().configureEach {
                    options.encoding = "UTF-8"
                }
                tasks.withType<JavaExec>().configureEach {
                    javaLauncher.set(toolchainService.launcherFor(javaExtension.toolchain))
                }
                tasks.withType<Checkstyle>().configureEach {
                    javaLauncher.set(toolchainService.launcherFor(javaExtension.toolchain))
                }
            }
        }
    }
}