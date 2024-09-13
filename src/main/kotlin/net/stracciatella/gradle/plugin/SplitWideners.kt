package net.stracciatella.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.BufferedWriter
import java.io.File
import javax.inject.Inject

abstract class SplitWideners : DefaultTask() {
    @get:Inject
    protected abstract val archiveOperations: ArchiveOperations

    @InputFile
    val inputArchive = project.objects.fileProperty()

    @Input
    val metaFileName = project.objects.property<String>()

    @Input
    val mergedWidenerFileName = project.objects.property<String>()

    @Nested
    val moduleConfiguration = project.objects.property<ModuleConfiguration>()

    @OutputDirectory
    val files = project.objects.directoryProperty()

    init {
        files.convention(project.layout.dir(project.provider { temporaryDir.resolve("wideners") }))
    }

    @TaskAction
    fun run() {
        val dir = files.get().asFile
        dir.deleteRecursively()
        dir.mkdirs()
        val zipTree = archiveOperations.zipTree(inputArchive)
        val metaFile = zipTree.matching { include(metaFileName.get()) }.singleFile
        val mergedWidenerFile = zipTree.matching { include(mergedWidenerFileName.get()) }.singleFile
        val metaReader = metaFile.bufferedReader()
        val widenerReader = mergedWidenerFile.bufferedReader()

        val outWriters = HashMap<String, BufferedWriter>()
        val outFiles = HashMap<String, File>()
        val header = widenerReader.readLine()!!
        moduleConfiguration.get().accessWideners.forEach {
            val file = dir.resolve(it)
            outFiles[it] = file
            val writer = file.bufferedWriter()
            outWriters[it] = writer
            writer.write(header)
            writer.newLine()
        }

        while (true) {
            val outFileName = metaReader.readLine()
            if (outFileName.isNullOrBlank()) break
            val outWriter = outWriters[outFileName]!!

            outWriter.write(widenerReader.readLine())
            outWriter.newLine()
        }

        outWriters.values.forEach { it.close() }
    }
}