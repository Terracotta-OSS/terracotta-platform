/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.util.Props;
import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.stringContainsInOrder;

public class ImportCommandIT extends BaseStartupIT {

  public ImportCommandIT() {
    super(1, 2);
  }

  @Before
  public void setUp() {
    forEachNode((stripeId, nodeId, port) -> startNode(
        "--node-name", "node-" + nodeId,
        "--node-hostname", "localhost",
        "--node-port", String.valueOf(port),
        "--node-group-port", String.valueOf(port + 10),
        "--node-log-dir", "logs/stripe" + stripeId + "/node-" + nodeId,
        "--node-backup-dir", "backup/stripe" + stripeId,
        "--node-metadata-dir", "metadata/stripe" + stripeId,
        "--node-repository-dir", "repository/stripe" + stripeId + "/node-" + nodeId,
        "--data-dirs", "main:user-data/main/stripe" + stripeId));

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));
  }

  @Test
  public void test_import() throws Exception {
    int[] ports = this.ports.getPorts();
    TreeMap<Object, Object> before = new TreeMap<>(getUpcomingCluster("localhost", ports[0]).toProperties());
    Path path = copyConfigProperty("/config-property-files/import.properties");
    ConfigTool.start("import", "-f", path.toString());
    assertCommandSuccessful();
    TreeMap<Object, Object> after = new TreeMap<>(getUpcomingCluster("localhost", ports[0]).toProperties());
    TreeMap<Object, Object> expected = new TreeMap<>(Props.load(path));
    assertThat(after, is(equalTo(expected)));
    assertThat(before, is(not(equalTo(expected))));
  }
}
