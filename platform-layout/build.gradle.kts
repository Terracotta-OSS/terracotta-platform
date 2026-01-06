@file:Suppress("UnstableApiUsage")

import org.gradle.api.attributes.DocsType.JAVADOC
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.util.internal.GUtil

import org.redundent.kotlin.xml.Namespace
import org.redundent.kotlin.xml.PrintOptions
import org.redundent.kotlin.xml.XmlVersion
import org.redundent.kotlin.xml.xml

import java.nio.file.FileSystems
import java.util.SortedMap
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream
import kotlin.text.Charsets.UTF_8

buildscript {
  dependencies {
    classpath("org.redundent:kotlin-xml-builder:1.9.1")
  }
}

plugins {
  id("jvm-ecosystem")
  id("org.terracotta.build.convention.distribution")
  id("org.terracotta.build.convention.deploy")
}

val kit by configurations.registering {
  description = "Outgoing kit configuration"
}

// tools

val tools by configurations.registering {
  description = "Tools (tools/)"
  attributes {
    attribute(Category.CATEGORY_ATTRIBUTE, objects.named<Category>(org.terracotta.build.plugins.ToolPlugin.TOOL_CATEGORY))
  }
}

// server libs

val serverLibs = configurations.dependencyScope("serverLibs") {
  description = "The server libraries"
}

val serverLibsClasspath = configurations.resolvable("serverLibsClasspath") {
  extendsFrom(serverLibs.get())
}

val serverLibsClasspathAssembly = tasks.register<ClasspathAssembly>("serverLibsClasspathAssembly") {
  classpath = serverLibsClasspath
  outputDirectory = layout.buildDirectory.dir("server-libs")
}

// plugins

val serverPluginApis by configurations.registering {
  description = "The server plugins apis (server/plugins/api)"
}

val serverClasspath = configurations.resolvable("serverClasspath") {
  extendsFrom(serverLibsClasspath.get(), serverPluginApis.get())
}

val serverPluginLibs = dependencies.extensions.create<ServerPluginExtension>("serverPluginLibs", project, serverClasspath)

// versions

val logbackVersion: String by properties
val slf4jVersion: String by properties
val terracottaCoreVersion: String by properties

dependencies {
  /*
   * These roundabout string invokes are necessary until Gradle 8.5 due to: https://github.com/gradle/gradle/issues/26602
   */
  serverLibs.name("org.terracotta.internal:server-runtime:$terracottaCoreVersion")

  // voltron server API
  serverPluginApis(project(":common:json"))
  serverPluginApis(project(":common:nomad"))
  serverPluginApis(project(":diagnostic:server:api"))
  serverPluginApis(project(":dynamic-config:server:api"))
  serverPluginApis(project(":management:server:api"))
  serverPluginApis(project(":security:logger:server:api"))

  // voltron resources
  serverPluginLibs
          .with(project(":resources:data-root"))
          .with(project(":resources:offheap"))

  // voltron services
  serverPluginLibs
          .with(project(":client-message-tracker"))
          .with(project(":diagnostic:server:services"))
          .with(project(":dynamic-config:server:config-provider")) // server config
          .with(project(":dynamic-config:server:services"))
          .with(project(":lease:server")) // also contains an entity
          .with(project(":management:server:services"))
          .with(project(":security:logger:server:services"))
          .with(project(":platform-base"))

  // voltron entities
  serverPluginLibs
          .with(project(":dynamic-config:entities:management:server"))
          .with(project(":dynamic-config:entities:nomad:server"))
          .with(project(":dynamic-config:entities:topology:server"))
          .with(project(":management:entities:nms-agent:server"))
          .with(project(":management:entities:nms:server"))

  tools(project(":dynamic-config:cli:config-tool"))
  tools(project(":voter"))
}

distributions {
  main {
    contents {
      includeEmptyDirs = false
      filesMatching(listOf("**/*.bat", "**/*.sh")) {
        permissions {
          unix("0775")
        }
      }
      filesNotMatching("**/*.jar") {
        duplicatesStrategy = DuplicatesStrategy.FAIL
      }
      filesMatching("**/*.jar") {
        // We can safely exclude JAR duplicates as our dependency strategy is fail on conflict
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
      }

      into("server") {
        into("plugins/api") {
          from(serverPluginApis) {
            exclude(filesIn(serverLibsClasspathAssembly))
          }
        }
        into("plugins/lib") {
          from(serverPluginLibs.files()) {
            exclude(filesIn(serverPluginApis, serverLibsClasspathAssembly))
          }
        }
      }

      into("tools") {
        from(tools)
      }
    }
  }
}

tasks.distTar {
  dependsOn(serverLibsClasspathAssembly)
  compression = Compression.GZIP
  archiveExtension = "tar.gz"
}

tasks.distZip {
  dependsOn(serverLibsClasspathAssembly)
}

val explodedKit = tasks.register<Sync>("explodedKit") {
  dependsOn(serverLibsClasspathAssembly)
  into(project.layout.buildDirectory.dir("exploded-kit"))
  with(distributions.main.get().contents)
}

artifacts {
  add(kit.name, explodedKit.map {
    copy -> copy.destinationDir
  })
}

publishing {
  publications {
    register<MavenPublication>("distribution") {
      artifact(tasks.distZip)
      artifact(tasks.distTar)
      groupId = "org.terracotta"
      artifactId = "terracotta-platform-layout"
      pom {
        name = "Terracotta platform layout"
        description = "Defines the Terracotta platform layout"
      }
    }
  }
}

fun filesIn(vararg files: Any): Spec<FileTreeElement>  {
  val filter: Provider<List<String>> = project.files(files).elements.map { fs ->
    fs.flatMap { f ->
      if (f.asFile.isDirectory) {
        fileTree(f.asFile).files
      } else {
        listOf(f.asFile)
      }
    }.map { f -> f.name }
  }
  return Spec { fte -> filter.get().contains(fte.name) }
}

abstract class ServerPluginExtension(private val project: Project, private val apis: Provider<Configuration>) {

  private val plugins: MutableCollection<Provider<out FileCollection>> = mutableListOf()

  fun files() : Provider<out FileCollection> {
    return plugins.stream().reduce {pa, pb -> pa.zip(pb) { a, b -> a.plus(b) }}.orElse(project.provider { project.files() })
  }

  fun with(notation: Any, config : Action<Dependency> = Action<Dependency>{}) : ServerPluginExtension {
    val dependency = project.dependencies.create(notation)
    config(dependency)

    val pluginConfigurationName = when (dependency) {
      is ModuleDependency -> "server-plugin#${dependency.group}#${dependency.name}${dependency.targetConfiguration?.let { "#${it}" } ?: ""}"
      else -> "server-plugin#${dependency.group}#${dependency.name}"
    }

    plugins += (project.configurations.register(pluginConfigurationName) {
      /*
       * This ensures that all the server plugins agree on the versions of the things that go in to the common
       * server api directory (and that the things in the server API directory are those things).
       */
      shouldResolveConsistentlyWith(apis.get())
      isVisible = false
      dependencies.add(dependency)
    })
    return this
  }
}

abstract class ClasspathAssembly : DefaultTask() {

  @get:InputFiles
  abstract val classpath: Property<Configuration>

  @get:OutputDirectory
  abstract val outputDirectory: DirectoryProperty

  @TaskAction
  fun assembleAndPatchManifest() {
    val classpath = classpath.get()

    project.sync {
      from(classpath)
      into(outputDirectory)
    }

    classpath.resolvedConfiguration.firstLevelModuleDependencies.forEach { dependency ->
      dependency.moduleArtifacts.forEach { artifact ->
        val file = artifact.file
        if (file.isFile && file.name.endsWith(".jar", ignoreCase = true)) {
          val targetFile = outputDirectory.file(file.name).get().asFile
          FileSystems.newFileSystem(project.uri("jar:" + targetFile.toURI()), emptyMap<String, String>()).use { jar ->
            val manifestEntry = jar.getPath("META-INF", "MANIFEST.MF")

            val manifest = manifestEntry.inputStream().use { java.util.jar.Manifest(it) }

            if (manifest.mainAttributes.containsKey(java.util.jar.Attributes.Name.CLASS_PATH)) {
              manifest.mainAttributes[java.util.jar.Attributes.Name.CLASS_PATH] = classpath.minus(file).joinToString(" ") { it.name }

              manifestEntry.outputStream().use {
                manifest.write(it)
              }
            }
          }
        } else if (file.isFile && file.name.endsWith(".zip", ignoreCase = true)) {
            project.sync {
                from(project.zipTree(file))
                into(outputDirectory)
            }
        }
      }
    }
  }
}
