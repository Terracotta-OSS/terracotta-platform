package org.terracotta.build.conventions;

import org.terracotta.build.plugins.BlacklistPlugin;
import org.gradle.api.Project;

/**
 * Convention plugin for dependency blacklisting.
 * This convention:
 * <ul>
 *     <li>blacklists {@code com.github.stefanbirkner:system-rules}</li>
 * </ul>
 */
public class BlacklistConvention implements ConventionPlugin<Project, BlacklistPlugin> {

  @Override
  public Class<BlacklistPlugin> isConventionFor() {
    return BlacklistPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(BlacklistPlugin.class);
    project.getExtensions().configure(BlacklistPlugin.BlacklistExtension.class, blacklist -> {
      //disallow these rules which create unexpected shared state between tests
      blacklist.test("com.github.stefanbirkner:system-rules");
    });
  }
}
