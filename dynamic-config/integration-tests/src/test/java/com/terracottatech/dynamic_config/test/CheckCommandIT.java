/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static java.util.Collections.emptyMap;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class CheckCommandIT extends BaseStartupIT {

  @Before
  public void setUp() throws Exception {
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

    waitedAssert(out::getLog, containsString("Started the server in diagnostic mode"));

    ConfigTool.start("activate", "-s", "localhost:" + ports.getPort(), "-n", "tc-cluster", "-l", licensePath().toString());
    out.clearLog();
  }

  @Test
  public void test_automatic_repair_after_commit_failure() throws Exception {
    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                containsString("Commit failed for server localhost:" + ports.getPort() + ". Reason: com.terracottatech.nomad.server.NomadException: java.lang.IllegalStateException: Simulate commit failure"))
                .and(containsString("Possible fix: The recovery process may need to be run")))));

    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), is(equalTo(emptyMap())));
    assertThat(getUpcomingCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));

    assertThat(
        () -> ConfigTool.start("set", "-s", "localhost:" + ports.getPort(), "-c", "stripe.1.node.1.tc-properties.com.terracottatech.dynamic-config.simulate=recover-needed"),
        is(throwing(instanceOf(IllegalStateException.class))
            .andMessage(both(
                stringContainsInOrder(Arrays.asList("Another change (with UUID ", " is already underway on ", ". It was started by ", " on ")))
                .and(containsString("Possible fix: The recovery process may need to be run")))));

    out.clearLog();
    ConfigTool.start("check", "-s", "localhost:" + ports.getPort());
    waitedAssert(out::getLog, containsString("Attempting an automatic repair to trigger commit phase again for uncommitted nodes"));

    assertThat(getRuntimeCluster("localhost", ports.getPort()).getSingleNode().get().getTcProperties(), hasEntry("com.terracottatech.dynamic-config.simulate", "recover-needed"));
  }
}
