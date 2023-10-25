package net.stracciatella.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

class StracciatellaPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            plugins.run {
                apply("java")
                apply("checkstyle")
                apply("fabric-loom")
            }
            // Libraries are downloaded from a repository
            val libraries = configurations.maybeCreate("stracciatellaLibrary")
            // Dependencies are dependencies on modules
            val dependencies = configurations.maybeCreate("stracciatellaDependency")
            val extension = ModuleConfiguration(this)
            extensions.add("stracciatella", extension)
            val generateModuleTask = tasks.register<GenerateModuleJson>("generateStracciatellaModuleJson") {
                directory.convention(layout.buildDirectory.dir("generated/module-json"))
                fileName.convention("stracciatella.module.json")
                moduleConfiguration.convention(provider {
                    extension.setDefaults(this@run, libraries, dependencies)
                    extension
                })
            }

            plugins.withType<JavaBasePlugin> {
                extensions.getByType<SourceSetContainer>().named(SourceSet.MAIN_SOURCE_SET_NAME) {
                    resources.srcDir(generateModuleTask)
                    configurations.named(compileClasspathConfigurationName) {
                        extendsFrom(libraries, dependencies)
                    }
                }
            }
        }
    }
}
