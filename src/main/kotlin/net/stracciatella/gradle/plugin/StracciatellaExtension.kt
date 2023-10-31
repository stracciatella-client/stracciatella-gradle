package net.stracciatella.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.register

class StracciatellaExtension {

    companion object {
        fun registerGenerator(
            project: Project,
            sourceSet: NamedDomainObjectProvider<SourceSet>
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            return registerGenerator(project, sourceSet) {}
        }

        fun registerGenerator(
            project: Project,
            sourceSet: NamedDomainObjectProvider<SourceSet>,
            moduleConfigurator: Action<ModuleConfiguration>
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            return registerGenerator(project, sourceSet, provider(project, moduleConfigurator))
        }

        fun registerGenerator(
            project: Project,
            sourceSet: NamedDomainObjectProvider<SourceSet>,
            moduleConfiguration: ModuleConfiguration
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            return registerGenerator(project, sourceSet, project.provider { moduleConfiguration })
        }

        fun registerGenerator(
            project: Project,
            sourceSet: NamedDomainObjectProvider<SourceSet>,
            moduleConfigurator: Provider<ModuleConfiguration>
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            val name = if (sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME) "" else sourceSet.name
            return registerGeneratorAndConfigurations(project, name, moduleConfigurator.map {
                if (it.id == null) it.id = sourceSet.name
                it
            }).apply {
                sourceSet.configure {
                    resources.srcDir(first)
                    project.configurations.named(compileClasspathConfigurationName).configure {
                        extendsFrom(second, third)
                    }
                }
            }
        }

        fun registerGeneratorAndConfigurations(
            project: Project,
            name: String,
            moduleConfigurator: Action<ModuleConfiguration>
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            return registerGeneratorAndConfigurations(project, name, provider(project, moduleConfigurator))
        }

        fun registerGeneratorAndConfigurations(
            project: Project,
            name: String,
            moduleConfigurator: Provider<ModuleConfiguration>
        ): Triple<TaskProvider<GenerateModuleJson>, Configuration, Configuration> {
            val stracciatella = if (name.isEmpty()) "stracciatella" else "Stracciatella"
            val libraries = project.configurations.create("${name}${stracciatella}Library")
            val dependencies = project.configurations.create("${name}${stracciatella}Dependency")
            return Triple(registerGenerator(project, "generateStracciatella${name}ModuleJson", moduleConfigurator.map {
                it.setDefaults(project, libraries, dependencies)
                it
            }), libraries, dependencies)
        }

        fun registerGenerator(
            project: Project,
            name: String,
            moduleConfigurator: Action<ModuleConfiguration>
        ): TaskProvider<GenerateModuleJson> {
            return registerGenerator(project, name, provider(project, moduleConfigurator))
        }

        fun registerGenerator(
            project: Project,
            name: String,
            moduleConfigurator: Provider<ModuleConfiguration>
        ): TaskProvider<GenerateModuleJson> {
            return project.tasks.register<GenerateModuleJson>(name) {
                directory.convention(project.layout.buildDirectory.dir("generated/module-json/$name"))
                fileName.convention("stracciatella.module.json")
                moduleConfiguration.convention(moduleConfigurator)
            }
        }

        private fun provider(project: Project, action: Action<ModuleConfiguration>): Provider<ModuleConfiguration> {
            return project.provider {
                ModuleConfiguration(project).apply {
                    action.execute(this)
                }
            }
        }
    }
}
