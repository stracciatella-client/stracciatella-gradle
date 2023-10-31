package net.stracciatella.gradle.plugin

import net.fabricmc.loom.bootstrap.LoomGradlePluginBootstrap
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class StracciatellaFabricPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.run {
            plugins.run {
                apply<StracciatellaBasePlugin>()
                apply<LoomGradlePluginBootstrap>()
            }
        }
    }
}