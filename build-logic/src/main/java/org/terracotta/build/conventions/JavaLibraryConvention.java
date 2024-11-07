package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaLibraryPlugin;

/**
 * Convention plugin for the {@code java-library} plugin.
 * This convention exists to allow for natural use of the {@link JavaConvention} plugin when desiring the
 * {@code java-library} plugin which indirectly applies the {@code java} plugin.
 */
public class JavaLibraryConvention implements ConventionPlugin<Project, JavaLibraryPlugin> {

  @Override
  public Class<JavaLibraryPlugin> isConventionFor() {
    return JavaLibraryPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaLibraryPlugin.class);
    project.getPlugins().apply(JavaConvention.class);
  }
}
