package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.terracotta.build.plugins.DeployPlugin;

/**
 * Convention plugin for the deploy plugin.
 */
public class DeployConvention implements ConventionPlugin<Project, DeployPlugin> {

  @Override
  public Class<DeployPlugin> isConventionFor() {
    return DeployPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BaseConvention.class);
    project.getPlugins().apply(DeployPlugin.class);
  }
}
