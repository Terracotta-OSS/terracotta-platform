package org.terracotta.build.conventions;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.lang.System.getProperty;
import static java.nio.file.Files.write;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.hamcrest.io.FileMatchers.anExistingFile;

public class BaseConventionTest {

  private static GradleRunner gradleRunnerWithLocalSetup(Path projectDir, String... arguments) {
    return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath().withArguments(concat(
        of("--gradle-user-home", getProperty("gradle-user-home")),
        of(arguments)).collect(toList()));
  }

  private static GradleRunner gradleRunner(Path projectDir, String... arguments) {
    return GradleRunner.create().withProjectDir(projectDir.toFile()).withPluginClasspath()
        .withArguments(arguments);
  }

  @Test
  public void testDefaultVersionBehavior(@TempDir Path projectDir) throws IOException {
    write(projectDir.resolve("gradle.properties"), asList("defaultVersion = 1.0.0"));

    write(projectDir.resolve("build.gradle"), asList(
        "plugins {",
        "  id 'org.terracotta.build.convention.base'",
        "}",
        "tasks.create('test').doFirst {",
        "  println \"version=${version}\"",
        "}"));

    GradleRunner gradle = gradleRunnerWithLocalSetup(projectDir, "test");

    BuildResult buildResult = gradle.build();

    assertThat(buildResult.getOutput(), containsString(
        "version=1.0.0"));
  }

  @Test
  public void testOverrideVersionBehavior(@TempDir Path projectDir) throws IOException {
    write(projectDir.resolve("gradle.properties"), asList("defaultVersion = 1.0.0"));

    write(projectDir.resolve("build.gradle"), asList(
        "plugins {",
        "  id 'org.terracotta.build.convention.base'",
        "}",
        "tasks.create('test').doFirst {",
        "  println \"version=${version}\"",
        "}"));

    GradleRunner gradle = gradleRunnerWithLocalSetup(projectDir, "-PoverrideVersion=2.0.0", "test");

    BuildResult buildResult = gradle.build();

    assertThat(buildResult.getOutput(), containsString(
        "version=2.0.0"));
  }

  @Test
  public void testConflictingDependencyVersionsFail(@TempDir Path projectDir) throws IOException {
    write(projectDir.resolve("build.gradle"), asList(
        "plugins {",
        "  id 'org.terracotta.build.convention.base'",
        "}",
        "configurations {",
        "  test",
        "}",
        "dependencies {",
        "  test 'ch.qos.logback:logback-core:1.2.3'",
        "  test 'ch.qos.logback:logback-classic:1.2.11'",
        "}"));

    GradleRunner gradle = gradleRunnerWithLocalSetup(projectDir, "dependencies", "--configuration", "test");

    BuildResult buildResult = gradle.buildAndFail();

    assertThat(buildResult.getOutput(), stringContainsInOrder(
        "Conflict", " found for the following module",
        " - ch.qos.logback:logback-core between versions 1.2.11 and 1.2.3"));
  }

  @Test
  public void testRegistersAllRepositoriesInTheCorrectOrder(@TempDir Path projectDir) throws IOException {
    write(projectDir.resolve("build.gradle"), asList(
        "plugins {",
        "  id 'org.terracotta.build.convention.base'",
        "}",
        "tasks.create('test').doFirst {",
        "  repositories.each {",
        "    println it.url",
        "  }",
        "}"));

    GradleRunner gradle = gradleRunner(projectDir, "test");

    BuildResult buildResult = gradle.build();

    assertThat(buildResult.getOutput(), stringContainsInOrder(
        "https://repo.maven.apache.org/maven2",
        "https://repo.terracotta.org/maven2"));
  }

  @Test
  public void testArtifactNamingIsLongForm(@TempDir Path projectDir) throws IOException {
    write(projectDir.resolve("file.txt"), asList("Hello World"));
    write(projectDir.resolve("settings.gradle"), asList(
        "rootProject.name = 'bar'"));
    write(projectDir.resolve("build.gradle"), asList(
        "plugins {",
        "  id 'org.terracotta.build.convention.base'",
        "}",
        "group = 'foo'",
        "tasks.register('test', Zip) {",
        "  from project.file('file.txt')",
        "}"));

    GradleRunner gradle = gradleRunnerWithLocalSetup(projectDir, "test");

    gradle.build();

    assertThat(projectDir.resolve(Paths.get("build", "distributions", "foo.bar.zip")).toFile(), is(anExistingFile()));
  }
}
