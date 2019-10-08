/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Json;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.nio.file.Paths;
import java.util.Arrays;

import static java.util.stream.IntStream.range;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class AttachDetachCommandIT extends BaseStartupIT {

  @Rule public ExpectedSystemExit systemExit = ExpectedSystemExit.none();

  @Before
  public void setUp() {
    int[] ports = this.ports.getPorts();

    range(0, ports.length).forEach(i -> startNode(
        "--cluster-name", "my-cluster",
        "--node-name", "node-" + (i + 1),
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(ports[i]),
        "--node-log-dir", "logs/stripe1/node-" + (i + 1),
        "--node-metadata-dir", "metadata/stripe1",
        "--node-repository-dir", "repository/stripe1/node-" + (i + 1),
        "--data-dirs", "main:user-data/main/stripe1"
    ));
    waitedAssert(out::getLog, stringContainsInOrder(Arrays.asList(
        "Started the server in diagnostic mode\n",
        "Started the server in diagnostic mode\n",
        "Started the server in diagnostic mode\n",
        "Started the server in diagnostic mode\n"
    )));
  }

  @Test
  public void test() {
    int[] ports = this.ports.getPorts();

    ConfigTool.main("export", "-s", "localhost:" + ports[0], "-o", "build/output.json", "-f", "json");
    Cluster cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));

    // add a node
    ConfigTool.main("attach", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[1]);
    ConfigTool.main("export", "-s", "localhost:" + ports[0], "-o", "build/output.json", "-f", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // add a stripe
    ConfigTool.main("attach", "-t", "stripe", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[2], "-s", "localhost:" + ports[3]);
    ConfigTool.main("export", "-s", "localhost:" + ports[0], "-o", "build/output.json", "-f", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodeAddresses(), hasSize(4));

    // remove the previously added stripe
    ConfigTool.main("detach", "-t", "stripe", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[2]);
    ConfigTool.main("export", "-s", "localhost:" + ports[0], "-o", "build/output.json", "-f", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(2));

    // remove the previously added node
    ConfigTool.main("detach", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[1]);
    ConfigTool.main("export", "-s", "localhost:" + ports[0], "-o", "build/output.json", "-f", "json");
    cluster = Json.parse(Paths.get("build", "output.json"), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodeAddresses(), hasSize(1));
  }

}
