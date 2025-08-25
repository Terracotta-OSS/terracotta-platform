package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.dsl.DependencyFactory;
import org.gradle.api.tasks.testing.Test;
import org.terracotta.build.plugins.GalvanPlugin;

import static org.terracotta.build.plugins.GalvanPlugin.FRAMEWORK_CONFIGURATION_NAME;

/**
 * Convention plugin for projects that run Galvan based tests.
 * This convention:
 * <ul>
 *     <li>picks the local enterprise galvan libraries over the open source ones the plugin configures</li>
 *     <li>configures the use of the locally built kit</li>
 *     <li>caps us at a maximum of 3 concurrent tests, and tweaks the test JVM arguments</li>
 * </ul>
 */
@SuppressWarnings("UnstableApiUsage")
public class GalvanConvention implements ConventionPlugin<Project, GalvanPlugin> {

  @Override
  public Class<GalvanPlugin> isConventionFor() {
    return GalvanPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(GalvanPlugin.class);

    DependencyFactory dependencyFactory = project.getDependencyFactory();

    project.getConfigurations().named(FRAMEWORK_CONFIGURATION_NAME, config -> {
      config.getDependencies().add(dependencyFactory.create(project.project(":dynamic-config:testing:galvan")));
    });

    project.getConfigurations().named("galvanKit", config -> {
      ProjectDependency projectDependency = dependencyFactory.create(project.project(":kit"));
      projectDependency.setTargetConfiguration("kit");
      config.getDependencies().add(projectDependency);
    });

    project.getTasks().withType(Test.class).configureEach(task -> {
      task.setMaxParallelForks(3);
      task.jvmArgs("-XX:MaxDirectMemorySize=1536m");
      task.jvmArgs("-XX:SoftRefLRUPolicyMSPerMB=0");
      task.jvmArgs("-Xmx1g");
      task.systemProperty("org.terracotta.disablePortReleaseCheck", "true");
    });
  }
}
