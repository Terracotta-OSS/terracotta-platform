package org.terracotta.build.conventions;

import org.gradle.api.Project;
import org.gradle.api.internal.HasConvention;
import org.gradle.api.provider.Provider;
import org.gradle.api.services.BuildService;
import org.gradle.api.services.BuildServiceParameters;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.Sync;
import org.gradle.api.tasks.TaskProvider;
import org.unbrokendome.gradle.plugins.xjc.XjcGenerate;
import org.unbrokendome.gradle.plugins.xjc.XjcPlugin;
import org.unbrokendome.gradle.plugins.xjc.XjcSourceSetConvention;

/**
 * Convention plugin for projects that use the XJC binding tool (via the 'org.unbroken-dome.xjc' plugin).
 * This convention:
 * <ul>
 *     <li>adds the XJC schema input to the source-set output so that it is available as a classpath resource</li>
 *     <li>constrains the XJC tasks to run one-at-a-time to avoid
 *     <a href="https://github.com/unbroken-dome/gradle-xjc-plugin/issues/33">gradle-xjc-plugin/issues/33</a></li>
 * </ul>
 */
@SuppressWarnings("deprecation") // Xjc plugin still uses convention objects
public class XjcConvention implements ConventionPlugin<Project, XjcPlugin> {

  @Override
  public Class<XjcPlugin> isConventionFor() {
    return XjcPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(XjcPlugin.class);

    project.getExtensions().configure(SourceSetContainer.class, sourceSets -> sourceSets.configureEach(sourceSet -> {
      XjcSourceSetConvention xjc = ((HasConvention) sourceSet).getConvention().getPlugin(XjcSourceSetConvention.class);
      TaskProvider<Sync> xjcSchemaSync = project.getTasks().register(sourceSet.getTaskName("xjcSchemaSync", null), Sync.class, sync -> {
        sync.from(xjc.getXjcSchema());
        sync.into(project.getLayout().getBuildDirectory().dir("generated/resources/xjc/schema/" + sourceSet.getName()));
      });
      sourceSet.getOutput().dir(xjcSchemaSync);
    }));

    Provider<XjcRunPermit> xjcTaskMutex = project.getGradle().getSharedServices()
            .registerIfAbsent("org.terracotta.xjc-task-mutex", XjcRunPermit.class, xjc -> xjc.getMaxParallelUsages().set(1));

    project.getTasks().withType(XjcGenerate.class).configureEach(task -> task.usesService(xjcTaskMutex));
  }

  public static abstract class XjcRunPermit implements BuildService<BuildServiceParameters.None> { }

}
