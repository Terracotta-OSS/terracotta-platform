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

val platformKit by configurations.registering {
}

// server libs

val serverLibs = configurations.dependencyScope("serverLibs") {
  description = "The server libraries"
}

val serverLibsClasspath = configurations.resolvable("serverLibsClasspath") {
  extendsFrom(serverLibs.get())
}

// versions

val logbackVersion: String by properties
val slf4jVersion: String by properties
val terracottaRuntimeVersion: String by properties

dependencies {
  serverLibs.name("org.terracotta.internal:server-runtime:$terracottaRuntimeVersion")
  platformKit(project(":platform-layout")) { 
    targetConfiguration = "kit" 
  }
/*
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
*/
}

val copyLibs = tasks.register("copyLibs") {
    val working = layout.buildDirectory.dir("server-libs").get()
    doLast {
        project.sync {
            from(serverLibsClasspath)
            into(working)
        }
    }
    outputs.dir(working)
}

val unzip = tasks.register<Copy>("unzip") {
  dependsOn(copyLibs)
  val working = layout.buildDirectory.dir("server-libs").get()
  working.getAsFileTree().matching {
    include("*.zip")
  } .forEach {
    from(zipTree(it))
  }
  into(working)
}

distributions {
  main {
    contents {
      includeEmptyDirs = false

      // platform layout
      into("") {
        from (platformKit.map { fileTree(it.singleFile) }) {
          include("**/*")
        }
      }

      // server libs
      into("server") {
        into("lib") {
          from(layout.buildDirectory.dir("server-libs")) {
            include("*.jar")
          }
        }
      }
    }
  }
}

tasks.distTar {
  dependsOn(unzip)
  dependsOn(":platform-layout:explodedKit")
  compression = Compression.GZIP
  archiveExtension = "tar.gz"
}

tasks.distZip {
  dependsOn(unzip)
  dependsOn(":platform-layout:explodedKit")
}

val explodedKit = tasks.register<Sync>("explodedKit") {
  dependsOn(unzip)
  dependsOn(":platform-layout:explodedKit")
  into(project.layout.buildDirectory.dir("exploded-kit"))
  with(distributions.main.get().contents)
}

artifacts {
  add(kit.name, explodedKit.map {
    copy -> copy.destinationDir
  })
}

