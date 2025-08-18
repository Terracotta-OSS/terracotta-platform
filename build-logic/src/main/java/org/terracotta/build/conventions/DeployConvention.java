package org.terracotta.build.conventions;

import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
import org.jfrog.gradle.plugin.artifactory.ArtifactoryPlugin;
import org.jfrog.gradle.plugin.artifactory.dsl.ArtifactoryPluginConvention;
import org.jfrog.gradle.plugin.artifactory.utils.ExtensionsUtils;
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
    project.getPlugins().apply(SigningPlugin.class);
    project.getPlugins().apply("maven-publish");
    project.getPlugins().apply(ArtifactoryPlugin.class);

    project.getTasks().getByPath("publish").dependsOn(project.getTasks().getByPath("artifactoryPublish"));

    ArtifactoryPluginConvention apc = ExtensionsUtils.getOrCreateArtifactoryExtension(project);
    apc.setContextUrl("https://na.artifactory.swg-devops.com/artifactory");
    apc.buildInfo(handler -> handler.setBuildName("hyc-webmriab-team-tc01/terracotta/" + project.getRootProject().getName()));
    apc.publish(config -> {
      config.defaults(defaults -> { defaults.publications("mavenJava", "distribution"); });
      config.repository(repo -> {
        repo.setReleaseRepoKey("hyc-webmriab-team-tc01-os-releases-maven-local");
        repo.setSnapshotRepoKey("hyc-webmriab-team-tc01-os-snapshots-maven-local");
        repo.setUsername(project.hasProperty("artifactory_user") ? String.valueOf(project.findProperty("artifactory_user")) : System.getenv("ARTIFACTORY_DEPLOY_USERNAME"));
        repo.setPassword(project.hasProperty("artifactory_password") ? String.valueOf(project.findProperty("artifactory_password")) : System.getenv("ARTIFACTORY_DEPLOY_PASSWORD"));
      });
    });

    final String gpgSigningKey = Optional.ofNullable(System.getenv("GPG_SIGNING_KEY"))
        .orElse(project.hasProperty("signingKey") ? project.property("signingKey").toString() : null);

    final String gpgSigningKeyId = Optional.ofNullable(System.getenv("GPG_SIGNING_KEY_ID"))
        .orElse(project.hasProperty("signingKeyId") ? project.property("signingKeyId").toString() : null);

    final String gpgSigningPassphrase = Optional.ofNullable(System.getenv("GPG_SIGNING_PASSPHRASE"))
        .orElse(project.hasProperty("signingPassword") ? project.property("signingPassword").toString() : null);

    if (gpgSigningKey != null) {
      project.afterEvaluate(p -> {
        p.getExtensions().configure(PublishingExtension.class, publishing -> {
          if (!publishing.getPublications().isEmpty()) {
            project.getExtensions().configure(SigningExtension.class, signing -> {
              if (gpgSigningKeyId != null) {
                signing.useInMemoryPgpKeys(gpgSigningKeyId, gpgSigningKey, gpgSigningPassphrase);
              } else {
                signing.useInMemoryPgpKeys(gpgSigningKey, gpgSigningPassphrase);
              }
              signing.sign(publishing.getPublications());
            });
          }
        });
      });
    }
  }
}
