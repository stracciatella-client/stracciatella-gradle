package net.stracciatella.gradle.plugin

import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register

class StracciatellaConfiguration(private val project: Project) : Runnable {
    override fun run() {
        val extension = ModuleConfiguration(project)
        project.extensions.add("stracciatella", extension)
        val dir = project.layout.buildDirectory.map { it.dir("stracciatella") }

        val triple =
            StracciatellaExtension.registerGeneratorAndConfigurations(project, "", project.provider { extension })

        val generateModuleJsonTask = triple.first
        val jarTask = project.tasks.named<Jar>("jar")
        val generateFabricData = project.tasks.register<GenerateFabricData>("generateFabricData") {
            dependsOn(jarTask)
            moduleConfiguration.set(extension)
            archiveIn.set(jarTask.flatMap { it.archiveFile })
        }
        val remapJarTask = project.tasks.named<RemapJarTask>("remapJar")
        // Loom already applied, configure jar and remapJar tasks
        val devJarTask = project.tasks.register<Jar>("devJarStracciatella") {
            from(project.zipTree(jarTask.map { it.archiveFile }))
            dependsOn(generateFabricData, generateModuleJsonTask)
            from(generateFabricData, generateModuleJsonTask)
            destinationDirectory.set(dir)
        }
        jarTask.configure {
            archiveClassifier.convention("dev")
            destinationDirectory.set(dir)
        }
        remapJarTask.configure {
            setDependsOn(listOf(devJarTask))
            dependsOn(generateFabricData)
            archiveClassifier.convention("remapped")
            inputFile.convention(devJarTask.flatMap { it.archiveFile })
            destinationDirectory.set(dir)
        }
        val splitWidenersTask = project.tasks.register<SplitWideners>("splitWideners") {
            dependsOn(remapJarTask)
            dependsOn(generateFabricData)
            moduleConfiguration.set(extension)
            inputArchive.set(remapJarTask.flatMap { it.archiveFile })
            metaFileName.set(generateFabricData.flatMap { it.fileNameWidenerMeta })
            mergedWidenerFileName.set(generateFabricData.flatMap { it.fileNameMergedWidener })
        }
        val completeJarTask = project.tasks.register<Jar>("completeJar") {
            dependsOn(remapJarTask)
            dependsOn(splitWidenersTask)
            dependsOn(generateFabricData)
            from(splitWidenersTask.map { it.files })
            from(project.zipTree(remapJarTask.map { it.archiveFile })) {
                exclude(generateFabricData.get().fileNameWidenerMeta.get())
                exclude(generateFabricData.get().fileNameMergedWidener.get())
                exclude(generateFabricData.get().fileNameModJson.get())
                exclude(extension.accessWideners)
            }
            duplicatesStrategy = DuplicatesStrategy.FAIL
        }
        project.tasks.named("assemble") {
            dependsOn(completeJarTask)
        }
        val sourceSets = project.extensions.getByType<SourceSetContainer>()
        sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME).run {
            project.configurations.named(compileClasspathConfigurationName).configure {
                extendsFrom(triple.second, triple.third)
            }
        }
    }
}