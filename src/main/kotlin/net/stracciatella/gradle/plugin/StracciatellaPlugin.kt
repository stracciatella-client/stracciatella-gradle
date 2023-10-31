package net.stracciatella.gradle.plugin

import net.stracciatella.gradle.plugin.StracciatellaExtension.Companion.registerGenerator
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.SourceSet.MAIN_SOURCE_SET_NAME
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getByType

class StracciatellaPlugin : Plugin<Project> {
    companion object {
        const val STRACCIATELLA_LIBRARY = "stracciatellaLibrary"
        const val STRACCIATELLA_DEPENDENCY = "stracciatellaDependency"
    }

    override fun apply(project: Project) {
        project.run {
            plugins.run {
                apply<StracciatellaFabricPlugin>()
            }
            val extension = ModuleConfiguration(this)
            extensions.add("stracciatella", extension)
            val sourceSets = extensions.getByType<SourceSetContainer>()
            registerGenerator(project, sourceSets.named(MAIN_SOURCE_SET_NAME), extension)
        }
    }
}
