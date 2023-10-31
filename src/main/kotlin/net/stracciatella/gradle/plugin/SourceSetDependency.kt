package net.stracciatella.gradle.plugin

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.FileCollectionDependency
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency
import org.gradle.api.internal.artifacts.dependencies.SelfResolvingDependencyInternal
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskDependency
import java.io.File

class SourceSetDependency private constructor(
    private val group: String,
    private val name: String,
    private val version: String,
    private val files: FileCollection,
    private val targetComponentId: ComponentIdentifier? = null
) : AbstractDependency(), FileCollectionDependency, SelfResolvingDependencyInternal {

    constructor(
        sourceSet: SourceSet,
        name: String = sourceSet.name,
        group: String = "net.stracciatella.sourcesets",
        version: String = "snapshot"
    ) : this(group, name, version, sourceSet.output.plus(sourceSet.compileClasspath))

    override fun getGroup(): String {
        return group
    }

    override fun getName(): String {
        return name
    }

    override fun getVersion(): String {
        return version
    }

    override fun contentEquals(dependency: Dependency): Boolean {
        return equals(dependency)
    }

    override fun copy(): Dependency {
        return SourceSetDependency(group, name, version, files)
    }

    override fun getBuildDependencies(): TaskDependency {
        return files.buildDependencies
    }

    override fun resolve(): MutableSet<File> {
        return files.files
    }

    override fun resolve(transitive: Boolean): MutableSet<File> {
        return files.files
    }

    override fun getTargetComponentId(): ComponentIdentifier? {
        return targetComponentId
    }

    override fun getFiles(): FileCollection {
        return files
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SourceSetDependency

        if (group != other.group) return false
        if (name != other.name) return false
        if (version != other.version) return false
        if (files != other.files) return false

        return true
    }

    override fun hashCode(): Int {
        var result = group.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + files.hashCode()
        return result
    }
}