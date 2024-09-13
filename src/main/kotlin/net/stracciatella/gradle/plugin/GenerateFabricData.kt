package net.stracciatella.gradle.plugin

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.property
import java.io.BufferedReader
import java.io.StringReader
import javax.inject.Inject

@CacheableTask
abstract class GenerateFabricData : DefaultTask() {
    @get:Inject
    abstract val archiveOperations: ArchiveOperations

    @Nested
    val moduleConfiguration: Property<ModuleConfiguration> = project.objects.property()

    @Input
    val fileNameModJson: Property<String> = project.objects.property()

    @Input
    val fileNameMergedWidener: Property<String> = project.objects.property()

    @Input
    val fileNameWidenerMeta: Property<String> = project.objects.property()

    @InputFile
    @PathSensitive(PathSensitivity.RELATIVE)
    val archiveIn: RegularFileProperty = project.objects.fileProperty()

    @OutputFiles
    val files: ConfigurableFileCollection = project.objects.fileCollection()

    private val fileModJson = project.objects.fileProperty()

    private val fileMergedWidener = project.objects.fileProperty()

    private val fileWidenerMeta = project.objects.fileProperty()

    init {
        fileNameModJson.convention("fabric.mod.json")
        fileNameMergedWidener.convention("stracciatella-generated.accesswidener")
        fileNameWidenerMeta.convention("stracciatella-generated.accesswidener.meta")
        fileModJson.convention { fileNameModJson.map { temporaryDir.resolve(it) }.get() }
        fileMergedWidener.convention { fileNameMergedWidener.map { temporaryDir.resolve(it) }.get() }
        fileWidenerMeta.convention { fileNameWidenerMeta.map { temporaryDir.resolve(it) }.get() }
        files.from(fileModJson)
        files.from(fileMergedWidener)
        files.from(fileWidenerMeta)
    }

    @TaskAction
    fun generate() {
        val moduleConfiguration = this.moduleConfiguration.get()
        moduleConfiguration.validate()
        val gson = GsonBuilder().setPrettyPrinting().serializeNulls().create()
        val modJsonWriter = fileModJson.get().asFile.bufferedWriter()
        val widenerWriter = fileMergedWidener.get().asFile.bufferedWriter()
        val metaWriter = fileWidenerMeta.get().asFile.bufferedWriter()
        val zipTree = archiveOperations.zipTree(archiveIn)
        val json = JsonObject()
        val mixins = JsonArray()
        moduleConfiguration.mixins.forEach { mixins.add(it) }
        val modId = "stracciatella-module-" + moduleConfiguration.id

        json.addProperty("schemaVersion", 1)
        json.addProperty("id", modId)
        json.addProperty("accessWidener", fileNameMergedWidener.get())
        json.add("mixins", mixins)
        widenerWriter.write("accessWidener v2 named")
        widenerWriter.newLine()
        for (accessWidener in moduleConfiguration.accessWideners) {
            val targetFile = zipTree.matching {
                include(accessWidener)
            }.singleFile
//            val targetFile = zipTree.filter { f ->
//                if (f.name != accessWidener) return@filter false
//                println(f.parentFile)
//                println(zipTree)
//                return@filter f.parentFile == archiveIn.asFile.get()
//            }.singleFile
            val content = targetFile.readBytes()
            val contentString = String(content, Charsets.UTF_8)
            val contentReader = BufferedReader(StringReader(contentString))
            val headerLine = contentReader.readLine()
            val header = headerLine.split(Regex("\\s+"))
            if (header.size != 3 || header[0] != "accessWidener") {
                throw RuntimeException("Invalid access widener file header. Expected: 'accessWidener <version> <namespace>', found '$headerLine'")
            }

            if (header[1] != "v2") {
                throw RuntimeException("Invalid access widener file version. Only v2 is currently supported, found " + header[1])
            }

            if (header[2] != "named") {
                throw RuntimeException("Invalid access widener file namespace. Only named is currently supported, found " + header[2])
            }
            while (true) {
                val line = contentReader.readLine() ?: break
                widenerWriter.write(line)
                widenerWriter.newLine()
                metaWriter.write(accessWidener)
                metaWriter.newLine()
            }
        }
        gson.toJson(json, modJsonWriter)
        metaWriter.close()
        modJsonWriter.close()
        widenerWriter.close()
    }
}