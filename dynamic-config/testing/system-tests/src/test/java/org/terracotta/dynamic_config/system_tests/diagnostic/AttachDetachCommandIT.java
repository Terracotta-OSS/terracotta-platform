/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.json.Json;

import java.io.IOException;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class AttachDetachCommandIT extends DynamicConfigIT {
  private static final String OUTPUT_JSON_FILE = "attach-detach-output.json";

  @Test
  public void test_attach_detach_with_unconfigured_nodes() throws Exception {
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    Cluster cluster = Json.parse(Paths.get("build", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));

    // add a node
    assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("build", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // add a stripe
    assertThat(configToolInvocation("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("build", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(3));

    // remove the previously added stripe
    assertThat(configToolInvocation("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("build", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // remove the previously added node
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configToolInvocation("export", "-s", "localhost:" + getNodePort(), "-f", OUTPUT_JSON_FILE, "-t", "json"), is(successful()));
    downloadToLocal();

    cluster = Json.parse(Paths.get("build", OUTPUT_JSON_FILE), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
  }

  private void downloadToLocal() throws IOException {
    tsa.browse(getNode(1, 1), ".").list().stream()
        .filter(remoteFile -> remoteFile.getName().equals(OUTPUT_JSON_FILE))
        .findFirst()
        .get()
        .downloadTo(Paths.get("build").resolve(OUTPUT_JSON_FILE).toFile());
  }
}
