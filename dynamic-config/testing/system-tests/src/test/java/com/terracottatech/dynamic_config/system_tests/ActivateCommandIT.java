/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.system_tests;

import com.terracottatech.dynamic_config.cli.config_tool.ConfigTool;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetWriterReader;
import com.terracottatech.store.Type;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.definition.CellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;
import com.terracottatech.store.manager.DatasetManager;
import org.junit.Before;
import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

public class ActivateCommandIT extends BaseStartupIT {

  public ActivateCommandIT() {
    super(2, 2);
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
  public void testMultiNodeSingleStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.start("attach", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[1]);
    assertCommandSuccessful();

    ConfigTool.start("activate", "-s", "localhost:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    waitedAssert(out::getLog, containsString("License installation successful"));
    waitedAssert(out::getLog, containsString("came back up"));
    assertCommandSuccessful();

    createDatasetAndPerformAssertions(ports);
  }

  @Test
  public void testMultiNodeSingleStripeActivationWithConfigFile() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.start(
        "-r", TIMEOUT + "s",
        "activate",
        "-f", copyConfigProperty("/config-property-files/single-stripe_multi-node.properties").toString(),
        "-l", licensePath().toString()
    );
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));

    waitedAssert(out::getLog, containsString("License installation successful"));
    waitedAssert(out::getLog, containsString("came back up"));
    assertCommandSuccessful();

    createDatasetAndPerformAssertions(ports);
  }

  @Test
  public void testMultiStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.start("attach", "-t", "stripe", "-d", "localhost:" + ports[0], "-s", "localhost:" + ports[2]);
    assertCommandSuccessful();

    ConfigTool.start("activate", "-s", "localhost:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
    ));

    waitedAssert(out::getLog, containsString("License installation successful"));
    waitedAssert(out::getLog, containsString("came back up"));
    assertCommandSuccessful();

    createDatasetAndPerformAssertions(ports);
  }

  @Test
  public void testMultiStripeActivationWithConfigFile() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.start(
        "-r", TIMEOUT + "s",
        "activate",
        "-f", copyConfigProperty("/config-property-files/multi-stripe.properties").toString(),
        "-l", licensePath().toString()
    );
    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
    ));

    waitedAssert(out::getLog, containsString("License installation successful"));
    waitedAssert(out::getLog, containsString("came back up"));
    assertCommandSuccessful();

    createDatasetAndPerformAssertions(ports);
  }

  private void createDatasetAndPerformAssertions(int... ports) throws Exception {
    Set<InetSocketAddress> node = Collections.singleton(InetSocketAddress.createUnresolved("localhost", ports[0]));
    try (DatasetManager datasetManager = DatasetManager.clustered(node).build()) {
      DatasetConfiguration offheapResource = datasetManager.datasetConfiguration().offheap("main").build();
      final String datasetName = "dataset";
      datasetManager.newDataset(datasetName, Type.LONG, offheapResource);
      try (Dataset<Long> rawDataset = datasetManager.getDataset(datasetName, Type.LONG)) {
        DatasetWriterReader<Long> myDataset = rawDataset.writerReader();
        StringCellDefinition NAME_CELL = CellDefinition.defineString("name");
        myDataset.add(123L, NAME_CELL.newCell("George"));
        assertThat(myDataset.get(123L).get().get(NAME_CELL).get(), is("George"));
      }
      datasetManager.destroyDataset(datasetName);
    }
  }
}