package net.stracciatella.gradle.plugin

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import javax.inject.Inject

@CacheableTask
abstract class GenerateModuleJson : DefaultTask() {
    @get:Inject
    abstract val repositoryHandler: RepositoryHandler

    @get:Nested
    abstract val moduleConfiguration: Property<ModuleConfiguration>

    @get:Input
    abstract val fileName: Property<String>

    @get:OutputDirectory
    abstract val directory: DirectoryProperty

    @TaskAction
    fun generate() {
        val moduleConfiguration = this.moduleConfiguration.get()
        moduleConfiguration.validate()
        val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
        val writer = directory.file(fileName).get().asFile.bufferedWriter()
        moduleConfiguration.resolveRepositories(repositoryHandler)
        gson.toJson(moduleConfiguration, writer)
        writer.close()
    }
}