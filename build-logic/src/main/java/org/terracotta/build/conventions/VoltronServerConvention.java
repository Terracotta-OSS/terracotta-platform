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
public class VoltronServerConvention implements ConventionPlugin<Project, VoltronPlugin> {

  @Override
  public Class<VoltronPlugin> isConventionFor() {
    return VoltronPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(VoltronPlugin.class);

    DependencyFactory dependencyFactory = project.getDependencyFactory();
    
    project.getConfigurations().named(VoltronPlugin.VOLTRON_CONFIGURATION_NAME, config -> {
      String terracottaCoreVersion = project.property("terracottaCoreVersion").toString();
      config.getDependencies().addAll(asList(
              dependencyFactory.create("org.terracotta", "server-api", terracottaCoreVersion)
      ));
    });

    project.getConfigurations().named(JavaPlugin.TEST_IMPLEMENTATION_CONFIGURATION_NAME, config -> {
      String slf4jVersion = project.property("slf4jVersion").toString();
      config.getDependencies().addAll(asList(
              dependencyFactory.create("org.slf4j", "slf4j-api", slf4jVersion)
      ));
    });
  }
}
