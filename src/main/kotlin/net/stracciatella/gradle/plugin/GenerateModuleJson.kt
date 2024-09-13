package net.stracciatella.gradle.plugin

import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import javax.inject.Inject

@CacheableTask
abstract class GenerateModuleJson : DefaultTask() {
    @get:Inject
    abstract val repositoryHandler: RepositoryHandler

    @Nested
    val moduleConfiguration: Property<ModuleConfiguration> = project.objects.property()

    @Input
    val fileName: Property<String> = project.objects.property()

    @OutputFiles
    val files: ConfigurableFileCollection = project.objects.fileCollection()

    private val moduleJsonFile = project.objects.fileProperty()

    init {
        moduleJsonFile.convention { fileName.map { temporaryDir.resolve(it) }.get() }
        files.from(moduleJsonFile)
        fileName.convention("stracciatella.module.json")
    }

    @TaskAction
    fun generate() {
        val moduleConfiguration = this.moduleConfiguration.get()
        moduleConfiguration.validate()
        val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()

        val writer = moduleJsonFile.get().asFile.bufferedWriter()

        moduleConfiguration.resolveRepositories(repositoryHandler)
        gson.toJson(moduleConfiguration, writer)
        writer.close()
    }
}