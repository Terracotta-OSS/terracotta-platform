package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin;
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension;
import org.terracotta.build.plugins.ToolPlugin;

/**
 * Convention plugin for the tool plugin.
 * This convention:
 * <ul>
 *     <li>adds this project to the list of projects the OWASP dependency checker will look at</li>
 * </ul>
 */
public class ToolConvention implements ConventionPlugin<Project, ToolPlugin> {

  @Override
  public Class<ToolPlugin> isConventionFor() {
    return ToolPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BaseConvention.class);
    project.getPlugins().apply(ToolPlugin.class);

    Project rootProject = project.getRootProject();
    rootProject.getPlugins().withType(DependencyCheckPlugin.class).configureEach(depCheckPlugin -> {
      rootProject.getExtensions().configure(DependencyCheckExtension.class, dependencyCheck -> {
        dependencyCheck.getScanProjects().add(project.getPath());
      });
    });
  }
}
