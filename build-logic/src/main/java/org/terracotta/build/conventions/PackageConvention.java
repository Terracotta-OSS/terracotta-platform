package org.terracotta.build.conventions;

import aQute.bnd.osgi.Constants;
import aQute.bnd.version.MavenVersion;
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar;
import org.gradle.api.Project;
import org.gradle.api.tasks.Sync;
import org.terracotta.build.plugins.PackagePlugin;
import org.terracotta.build.plugins.buildinfo.StructuredVersion;
import org.terracotta.build.plugins.packaging.OsgiManifestJarExtension;

import static org.terracotta.build.PluginUtils.capitalize;
import static org.terracotta.build.Utils.mapOf;
import static org.terracotta.build.plugins.packaging.PackageInternal.SOURCES_TASK_NAME;
import static org.terracotta.build.plugins.packaging.PackageInternal.UNPACKAGED_API_ELEMENTS_CONFIGURATION_NAME;

/**
 * Convention plugin for our custom packaging plugin.
 * This convention:
 * <ul>
 *     <li>enforces our standard relocation rules on packages shared between 4.x and 10.x</li>
 *     <li>sets the OSGi bundle version to the iBit {@code 1.2.3.0004-0005} convention</li>
 *     <li>excludes the relocated modules from all the unpackaged API elements configurations -
 *     this prevents conflict between API and runtime classpaths</li>
 *     <li>excludes license related files from package inputs and includes the local NOTICE file</li>
 * </ul>
 */
public class PackageConvention implements ConventionPlugin<Project, PackagePlugin>  {
  @Override
  public Class<PackagePlugin> isConventionFor() {
    return PackagePlugin.class;
  }

  @Override
  public void apply(Project project) {
    project.getPlugins().apply(JavaBaseConvention.class);
    project.getPlugins().apply(PackagePlugin.class);

    project.getTasks().withType(ShadowJar.class).configureEach(shadow -> {
      shadow.exclude("LICENSE", "NOTICE");
      shadow.from(project.getRootProject().file("NOTICE"));

      shadow.relocate("org.terracotta.statistics.", "org.terracotta.shadow.org.terracotta.statistics.");
      shadow.relocate("org.terracotta.context.", "org.terracotta.shadow.org.terracotta.context.");
      shadow.relocate("org.terracotta.offheapstore.", "org.terracotta.shadow.org.terracotta.offheapstore.");
      shadow.relocate("org.terracotta.offheapstore.", "org.terracotta.shadow.org.terracotta.offheapstore.");
      shadow.relocate("org.terracotta.frs.", "org.terracotta.shadow.org.terracotta.frs.");

      shadow.getExtensions().configure(OsgiManifestJarExtension.class, manifest -> {
        String sagVersion = BaseConvention.toSagVersion((StructuredVersion) project.getVersion());
        manifest.instruction(Constants.BUNDLE_VERSION, new MavenVersion(sagVersion).getOSGiVersion().toString());
      });
    });

    //exclude shaded packages from the unpackaged api elements
    project.getConfigurations().matching(s -> s.getName().equals(UNPACKAGED_API_ELEMENTS_CONFIGURATION_NAME)
            || s.getName().endsWith(capitalize(UNPACKAGED_API_ELEMENTS_CONFIGURATION_NAME))).configureEach(config -> {
      config.exclude(mapOf(String.class, String.class, "group", "org.terracotta", "module", "statistics"));
      config.exclude(mapOf(String.class, String.class, "group", "org.terracotta", "module", "offheap-store"));
      config.exclude(mapOf(String.class, String.class, "group", "org.terracotta", "module", "fast-restartable-store"));
    });

    project.getTasks().withType(Sync.class).matching(s -> s.getName().equals(SOURCES_TASK_NAME)
            || s.getName().endsWith(capitalize(SOURCES_TASK_NAME))).configureEach(sources -> {
      sources.exclude("LICENSE", "NOTICE");
    });

  }
}
