package org.terracotta.build.conventions;

import org.terracotta.build.plugins.VoltronPlugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.dsl.DependencyFactory;

import static java.util.Arrays.asList;

/**
 * Convention plugin for Voltron resident libraries.
 * This convention adds the standard set of Voltron provided libraries to the 'voltron' dependency scope.
 */
@SuppressWarnings("UnstableApiUsage")
public class VoltronConvention implements ConventionPlugin<Project, VoltronPlugin> {

  @Override
  public Class<VoltronPlugin> isConventionFor() {
    return VoltronPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(VoltronPlugin.class);

    DependencyFactory dependencyFactory = project.getDependencyFactory();

    project.getConfigurations().named(VoltronPlugin.VOLTRON_CONFIGURATION_NAME, config -> {
      String terracottaApisVersion = project.property("terracottaApisVersion").toString();
      String slf4jVersion = project.property("slf4jVersion").toString();
      config.getDependencies().addAll(asList(
              dependencyFactory.create("org.terracotta", "entity-server-api", terracottaApisVersion),
              dependencyFactory.create("org.terracotta", "standard-cluster-services", terracottaApisVersion),
              dependencyFactory.create("org.terracotta", "packaging-support", terracottaApisVersion),
              dependencyFactory.create("org.slf4j", "slf4j-api", slf4jVersion)
      ));
    });
  }
}
