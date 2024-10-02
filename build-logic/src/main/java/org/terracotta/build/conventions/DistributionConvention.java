package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.gradle.api.distribution.DistributionContainer;
import org.gradle.api.distribution.plugins.DistributionPlugin;
import org.owasp.dependencycheck.gradle.DependencyCheckPlugin;
import org.owasp.dependencycheck.gradle.extension.DependencyCheckExtension;

/**
 * Convention plugin for the distribution plugin.
 * This convention:
 * <ul>
 *     <li>adds this project to the list of projects the OWASP dependency checker will look at</li>
 *     <li>adds the {@code /LICENSE} file to the root of all distributions</li>
 * </ul>
 */
public class DistributionConvention implements ConventionPlugin<Project, DistributionPlugin> {

  @Override
  public Class<DistributionPlugin> isConventionFor() {
    return DistributionPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BaseConvention.class);
    project.getPlugins().apply(DistributionPlugin.class);

    Project rootProject = project.getRootProject();
    rootProject.getPlugins().withType(DependencyCheckPlugin.class).configureEach(depCheckPlugin -> {
      rootProject.getExtensions().configure(DependencyCheckExtension.class, dependencyCheck -> {
        dependencyCheck.getScanProjects().add(project.getPath());
      });
    });

    // Add license to all distributions
    project.getExtensions().configure(DistributionContainer.class, distributions -> distributions.configureEach(
            distribution -> distribution.getContents().from(project.getRootProject().file("LICENSE")))
    );
  }
}
