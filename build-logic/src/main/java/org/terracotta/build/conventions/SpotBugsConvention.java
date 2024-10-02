package org.terracotta.build.conventions;

import com.github.spotbugs.snom.SpotBugsExtension;
import com.github.spotbugs.snom.SpotBugsPlugin;
import com.github.spotbugs.snom.SpotBugsTask;
import org.terracotta.build.plugins.JavaVersionPlugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JvmEcosystemPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;

import java.util.Locale;

import static org.gradle.language.base.plugins.LifecycleBasePlugin.VERIFICATION_GROUP;

/**
 * Convention plugin for the SpotBugs (aka FindBugs) static analysis tool.
 * This convention:
 * <ul>
 *     <li>forces the underlying SpotBugs library to 4.2.0 due to dumb heuristics in later versions</li>
 *     <li>adds the SpotBugs annotations to every source sets compile-only scope</li>
 *     <li>registers XML & HTML reports, but only enable the html (XML is used in CI)</li>
 *     <li>registers the {@code spotbugs} uber-task that depends on all the spotbugs analysis tasks</li>
 * </ul>
 */
public class SpotBugsConvention implements ConventionPlugin<Project, SpotBugsPlugin> {

  @Override
  public Class<SpotBugsPlugin> isConventionFor() {
    return SpotBugsPlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(SpotBugsPlugin.class);

    SpotBugsExtension spotbugs = project.getExtensions().getByType(SpotBugsExtension.class);

    spotbugs.getToolVersion().value("4.8.6");
    /*
     * Spotbugs 4.3.0 introduced a new "explicit immutable list" approach to identifying mutable types. This results in
     * an explosion of EI_EXPOSE_REP warnings (and it's derivatives). Some of these are correct some are not, but it's
     * all compounded by SpotBugs not understanding simple idioms like `unmodifiableList(...)` as fixes. We're just going
     * to suppress that detector until someone in SpotBugs wises up.
     */
    spotbugs.getOmitVisitors().add("FindReturnRef");

    project.getPlugins().withType(JvmEcosystemPlugin.class).configureEach(plugin ->
            project.getExtensions().configure(SourceSetContainer.class, sourceSets -> sourceSets.configureEach(sourceSet -> {
              project.getDependencies().addProvider(sourceSet.getCompileOnlyConfigurationName(), spotbugs.getToolVersion().map(version -> "com.github.spotbugs:spotbugs-annotations:" + version));

              if (sourceSet.getName().toLowerCase(Locale.ROOT).contains("test")) {
                project.getTasks().named(sourceSet.getTaskName("spotbugs", null), SpotBugsTask.class).configure(task -> task.setEnabled(false));
              }
            }))
    );

    project.getPlugins().withType(JavaVersionPlugin.class).configureEach(plugin -> {
      project.getExtensions().configure(JavaVersionPlugin.JavaVersions.class, javaVersions -> {
        Provider<JavaLauncher> compileJavaLauncher = project.getExtensions().getByType(JavaToolchainService.class)
                .launcherFor(spec -> spec.getLanguageVersion().convention(javaVersions.getCompileVersion()));
        project.getTasks().withType(SpotBugsTask.class).configureEach(task -> task.getLauncher().set(compileJavaLauncher));
      });
    });

    project.getTasks().withType(SpotBugsTask.class).configureEach(task -> task.reports(reports -> {
      reports.register("xml", xml -> xml.getRequired().set(false));
      reports.register("html", html -> html.getRequired().set(true));
    }));

    project.getTasks().register("spotbugs", task -> {
      task.setDescription("Run SpotBugs analysis");
      task.setGroup(VERIFICATION_GROUP);
      task.dependsOn(project.getTasks().withType(SpotBugsTask.class));
    });
  }
}
