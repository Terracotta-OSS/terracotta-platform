package org.terracotta.build.conventions;

import org.terracotta.build.plugins.VoltronPlugin;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyFactory;

import static java.util.Arrays.asList;

/**
 * Convention plugin for Voltron resident libraries.
 * This convention adds the standard set of Voltron provided libraries to the 'voltron' dependency scope.
 */
@SuppressWarnings("UnstableApiUsage")
public class VoltronClientConvention implements ConventionPlugin<Project, VoltronPlugin> {

  @Override
  public Class<VoltronPlugin> isConventionFor() {
    return VoltronPlugin.class;
  }

  @Override
  public void apply(Project project) {
    DependencyFactory dependencyFactory = project.getDependencyFactory();

    String terracottaCoreVersion = project.property("terracottaCoreVersion").toString();
    project.getConfigurations().named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, config -> {
      config.getDependencies().addAll(asList(
              dependencyFactory.create("org.terracotta", "client-api", terracottaCoreVersion)
      ));
    });

    project.getConfigurations().named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, config -> {
      String slf4jVersion = project.property("slf4jVersion").toString();
      config.getDependencies().addAll(asList(
              dependencyFactory.create("org.terracotta", "client-api", terracottaCoreVersion),
              dependencyFactory.create("org.slf4j", "slf4j-api", slf4jVersion)
      ));
    });
  }
}
