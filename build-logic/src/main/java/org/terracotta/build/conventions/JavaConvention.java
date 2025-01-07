package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.PluginContainer;

/**
 * Convention plugin for the {@code java} plugin.
 * This convention:
 * <ul>
 *     <li>enables source jar generation</li>
 *     <li>adds {@code org.slf4j:slf4j-api} to the implementation scope</li>
 * </ul>
 */
public class JavaConvention implements ConventionPlugin<Project, JavaPlugin> {

  @Override
  public Class<JavaPlugin> isConventionFor() {
    return JavaPlugin.class;
  }

  @Override
  public void apply(Project project) {
    PluginContainer plugins = project.getPlugins();
    plugins.apply(JavaPlugin.class);
    plugins.apply(JavaBaseConvention.class);

    project.getExtensions().configure(JavaPluginExtension.class, JavaPluginExtension::withSourcesJar);

    project.getDependencies().add(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME, "org.slf4j:slf4j-api:" + project.property("slf4jVersion"));
  }
}
