package org.terracotta.build.conventions;

import java.util.Optional;

import org.gradle.api.Project;
import org.gradle.api.publish.PublishingExtension;
import org.gradle.plugins.signing.SigningExtension;
import org.gradle.plugins.signing.SigningPlugin;
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
