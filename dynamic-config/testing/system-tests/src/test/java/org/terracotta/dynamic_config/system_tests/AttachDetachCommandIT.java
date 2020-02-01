/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.cli.config_tool.ConfigTool;
import org.terracotta.json.Json;

import java.nio.file.Paths;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2)
public class AttachDetachCommandIT extends DynamicConfigIT {
  @Test
  public void test() {
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    Cluster cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));

    // add a node
    ConfigTool.start("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // add a stripe
    ConfigTool.start("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1), "-s", "localhost:" + getNodePort(2, 2));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(4));

    // remove the previously added stripe
    ConfigTool.start("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // remove the previously added node
    ConfigTool.start("detach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2));
    ConfigTool.start("export", "-s", "localhost:" + getNodePort(), "-f", "build/output.json", "-t", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
  }
}
