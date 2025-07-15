package org.terracotta.build.conventions;

import org.terracotta.build.plugins.AngelaPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.tasks.testing.Test;

import static org.terracotta.build.Utils.group;
import static org.terracotta.build.plugins.AngelaPlugin.FRAMEWORK_CONFIGURATION_NAME;

/**
 * Convention plugin for projects that run Angela based tests.
 * This convention:
 * <ul>
 *     <li>picks the version of Angela defined by the {@code angelaVersion} Gradle property</li>
 *     <li>adds the local `angela-test-kit-resolver` to the classpath and configured the use of the locally built kit</li>
 *     <li>caps us at a maximum of 3 concurrent tests, and tweaks the test JVM arguments</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class AngelaConvention implements ConventionPlugin<Project, AngelaPlugin> {

  @Override
  public Class<AngelaPlugin> isConventionFor() {
    return AngelaPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(AngelaPlugin.class);

    DependencyFactory dependencyFactory = project.getDependencyFactory();

    ModuleDependency angela = dependencyFactory.create("org.terracotta", "angela", project.property("terracottaAngelaVersion").toString())
            .exclude(group("org.slf4j")).exclude(group("javax.cache"));

    project.getConfigurations().named(FRAMEWORK_CONFIGURATION_NAME, config -> {
      config.getDependencies().add(angela);
    });

    project.getConfigurations().named("angelaKit", config -> {
      ProjectDependency projectDependency = dependencyFactory.create(project.project(":kit"));
      projectDependency.setTargetConfiguration("kit");
      config.getDependencies().add(projectDependency);
    });

    project.getTasks().withType(Test.class).configureEach(task -> {
      task.setMaxParallelForks(3);
      task.jvmArgs("-XX:MaxDirectMemorySize=1536m");
      task.jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=0");
      task.jvmArgs("-Xmx1g");
      task.systemProperty("angela.skipUninstall", "false");
      task.systemProperty("org.terracotta.disablePortReleaseCheck", "true");
    });
  }
}
