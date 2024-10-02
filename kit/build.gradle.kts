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
}

val kit by configurations.registering {
  description = "Outgoing kit configuration"
}

// layouts: 
// - core == core layout from terracotta-core project
// - platform == platform layout from this project

val coreKit by configurations.registering {
}
val platformKit by configurations.registering {
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

// versions

val logbackVersion: String by properties
val slf4jVersion: String by properties
val terracottaCoreVersion: String by properties

dependencies {
  coreKit("org.terracotta.internal:terracotta-kit:$terracottaCoreVersion@tar.gz")
  platformKit(project(":platform-layout")) { 
    targetConfiguration = "kit" 
  }

  serverLibs.name("org.terracotta.internal:terracotta:$terracottaCoreVersion")
  serverLibs.name("org.terracotta.internal:tc-server:$terracottaCoreVersion")

  constraints {
    serverLibs.name("ch.qos.logback:logback-classic") {
      version {
        strictly(logbackVersion)
      }
    }
    serverLibs.name("org.slf4j:slf4j-api") {
      version {
        strictly(slf4jVersion)
      }
    }
  }
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

      // core layout
      into("") {
        from (coreKit.map { tarTree(it.singleFile) }) {
          include("*/server/bin/**")
          include("*/init/**")
          include("*/legal/**")

          // drop <dist-name>
          eachFile(org.terracotta.build.Utils.dropTopLevelDirectories(1))
        }
      }

      // platform layout
      into("") {
        from (platformKit.map { fileTree(it.singleFile) }) {
          include("**/*")
        }
      }

      // server libs
      // (ideally should come from core layout but we need to make sure we have the right versions of the dependencies)
      into("server") {
        into("lib") {
          from(serverLibsClasspathAssembly) {
            rename("terracotta-$terracottaCoreVersion.jar", "tc.jar")
          }
        }
      }
    }
  }
}

tasks.distTar {
  dependsOn(serverLibsClasspathAssembly)
  dependsOn(":platform-layout:explodedKit")
  compression = Compression.GZIP
  archiveExtension = "tar.gz"
}

tasks.distZip {
  dependsOn(serverLibsClasspathAssembly)
  dependsOn(":platform-layout:explodedKit")
}

val explodedKit = tasks.register<Sync>("explodedKit") {
  dependsOn(serverLibsClasspathAssembly)
  dependsOn(":platform-layout:explodedKit")
  into(project.layout.buildDirectory.dir("exploded-kit"))
  with(distributions.main.get().contents)
}

artifacts {
  add(kit.name, explodedKit.map {
    copy -> copy.destinationDir
  })
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
        }
      }
    }
  }
}
