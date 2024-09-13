package net.stracciatella.gradle.plugin

import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.configuration.accesswidener.AccessWidenerJarProcessor
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.artifacts.repositories.resolver.MavenUniqueSnapshotComponentIdentifier
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

open class ModuleConfiguration(@Transient private val project: Project) {
    companion object {
        private val loomProcessorIndex: AtomicInteger = AtomicInteger()
    }

    @Transient
    private val dependencyIndex: AtomicInteger = AtomicInteger()

    @Transient
    private val resolved: AtomicBoolean = AtomicBoolean()

    @Input
    @Optional
    var main: String? = null

    @Input
    @Optional
    var group: String? = null

    @Input
    @Optional
    var version: String? = project.version.toString()

    @Input
    @Optional
    var name: String? = null

    @Input
    @Optional
    var website: String? = null

    @Input
    @Optional
    var description: String? = null

    @Input
    @Optional
    var id: String? = null

    @Input
    val mixins: MutableSet<String> = HashSet()

    @Input
    val accessWideners: MutableSet<String> = HashSet()

    @Input
    @Optional
    val authors: MutableList<String> = ArrayList()

    @Nested
    val dependencies: MutableMap<String, Dependency> = HashMap()

    @Nested
    val repositories: MutableMap<String, Repository> = HashMap()

    data class Repository(@Input val name: String) {
        @Input
        var url: String? = null
    }

    data class Dependency(@Input val name: String) {
        constructor(
            group: String? = null,
            name: String,
            version: String? = null,
            type: String = "default",
            checksum: String? = null,
            optional: Boolean = false
        ) : this(name) {
            this.group = group
            this.version = version
            this.type = type
            this.checksum = checksum
            this.optional = optional
        }

        @Input
        var group: String? = null

        @Input
        var version: String? = null

        @Input
        var type: String = "default"

        @Input
        @Optional
        var checksum: String? = null

        @Input
        @Optional
        var repository: String? = null

        @Input
        var optional: Boolean = false

        @Input
        @Transient
        var needsRepoResolve: Boolean = true
    }

    fun mixin(mixin: String) {
        mixins.add(mixin)
    }

    fun accessWidener(accessWidener: String) {
        accessWidener(accessWidener, "src/main/resources/$accessWidener")
    }

    fun accessWidener(accessWidener: String, file: Any) {
        accessWideners.add(accessWidener)
        (project.extensions.findByName("loom") as LoomGradleExtension?)?.run {
            val property = project.objects.fileProperty().convention { project.file(file) }
            addMinecraftJarProcessor(
                AccessWidenerJarProcessor::class.java,
                "stracciatella-${loomProcessorIndex.incrementAndGet()}",
                true,
                property
            )
        }
    }

    internal fun setDefaults(project: Project, libraries: Configuration, dependencies: Configuration) {
        if (resolved.compareAndSet(false, true)) {
            name = name ?: project.name
            group = group ?: project.group.toString()
            version = version ?: project.version.toString()
            website = website ?: "https://github.com/stracciatella-client/"
            description = description ?: project.description ?: "A Stracciatella module"

            libraries.resolvedConfiguration.resolvedArtifacts.forEach {
                val dependency = resolveDependency(it, it.moduleVersion.id)
                this.dependencies[dependencyKey(dependency)] = dependency
            }
            dependencies.run {
                resolve()
                this.dependencies.forEach {
                    val generated: Boolean = it.name == "unspecified"
                    val name = if (generated) "generated-${dependencyIndex.incrementAndGet()}" else it.name
                    val group = if (generated) "net.stracciatella.generated" else it.group
                    val version = if (generated) "generated" else it.version
                    val type = "module"
                    val dependency = Dependency(group, name, version, type, optional = generated)
                    dependency.needsRepoResolve = false
                    this@ModuleConfiguration.dependencies[dependencyKey(dependency)] = dependency
                }
            }
        }
    }

    fun dependencyKey(dependency: Dependency): String {
        if (dependency.type == "module") return dependency.name
        return "${dependency.group}:${dependency.name}:${dependency.version}"
    }

    private fun resolveDependency(art: ResolvedArtifact, id: ModuleVersionIdentifier): Dependency {
        var version = id.version
        if (id.version.endsWith("-SNAPSHOT") && art.id.componentIdentifier is MavenUniqueSnapshotComponentIdentifier) {
            version = (art.id.componentIdentifier as MavenUniqueSnapshotComponentIdentifier).timestampedVersion
        }

        val dependency = Dependency(id.name);
        dependency.group = id.group
        dependency.version = version
        dependency.checksum = fileShaSum(art.file)

        return dependency
    }

    @Throws(IOException::class)
    private fun fileShaSum(path: File): String {
        return newSha3256Digest().run {
            update(path.readBytes())
            bytesToHex(digest())
        }
    }

    @Throws(NoSuchAlgorithmException::class)
    private fun newSha3256Digest(): MessageDigest = MessageDigest.getInstance("SHA3-256")

    private fun bytesToHex(input: ByteArray): String {
        val buffer = StringBuilder()
        for (b in input) {
            buffer.append(Character.forDigit(b.toInt() shr 4 and 0xF, 16))
            buffer.append(Character.forDigit(b.toInt() and 0xF, 16))
        }
        return buffer.toString()
    }

    internal fun validate() {
        if (main.isNullOrBlank()) throw InvalidModuleJson("no module main class")
        if (name.isNullOrBlank()) throw InvalidModuleJson("no module name")
        if (group.isNullOrBlank()) throw InvalidModuleJson("no module group")
        if (id.isNullOrBlank()) throw InvalidModuleJson("no module id")
    }

    internal fun resolveRepositories(repositoryHandler: RepositoryHandler) {
        val repos = repositoryHandler.filterIsInstance<MavenArtifactRepository>()
        dependencies.values.filter {
            it.needsRepoResolve
        }.forEach {
            val repo = resolveRepository(it, repos) ?: throw UnknownDependencyException(it)
            it.repository = repo.name
            val repository = Repository(repo.name)
            repository.url = repo.url.toURL().toExternalForm()
            repositories[repository.name] = repository
        }
    }

    private fun resolveRepository(
        dep: Dependency,
        repositories: Iterable<MavenArtifactRepository>
    ): MavenArtifactRepository? {
        return repositories.firstOrNull {
            val repoURLString = it.url.toString()
            val url = URI.create(
                "$repoURLString${if (repoURLString.endsWith('/')) "" else "/"}${
                    dep.group!!.replace(
                        '.',
                        '/'
                    )
                }/${dep.name}/${dep.version}/${dep.name}-${dep.version}.jar"
            ).toURL()
            val connection = url.openConnection()
            if (connection is HttpURLConnection) {
                return@firstOrNull with(connection) {
                    useCaches = false
                    readTimeout = 30000
                    connectTimeout = 30000
                    instanceFollowRedirects = true

                    setRequestProperty(
                        "User-Agent",
                        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11"
                    )

                    connect()
                    responseCode == 200
                }
            }
            false
        }
    }

    override fun toString(): String {
        return "ModuleConfiguration(main=$main, group=$group, version=$version, name=$name, website=$website, description=$description, id=$id, mixins=$mixins, accessWideners=$accessWideners, authors=$authors, dependencies=$dependencies, repositories=$repositories)"
    }
}
