/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test;

import com.terracottatech.dynamic_config.cli.ConfigTool;
import com.terracottatech.dynamic_config.test.util.Kit;
import com.terracottatech.dynamic_config.test.util.NodeProcess;
import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetWriterReader;
import com.terracottatech.store.Type;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.definition.CellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;
import com.terracottatech.store.manager.DatasetManager;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.junit.Assert.assertThat;

public class ActivateIT extends BaseStartupIT {
  @Rule
  public ExpectedSystemExit systemExit = ExpectedSystemExit.none();

  @Before
  public void setUp() {
    int[] ports = this.ports.getPorts();
    // Ensure that there are even number of ports before we start the test
    assertThat(NODE_COUNT, greaterThanOrEqualTo(2));
    assertThat(NODE_COUNT % 2, is(0));

    for (int i = 0; i < NODE_COUNT; i += 2) {
      int port = ports[i];
      int groupPort = ports[i + 1];
      nodeProcesses.add(NodeProcess.startNode(
          Kit.getOrCreatePath(),
          "--node-hostname", "localhost",
          "--node-port", String.valueOf(port),
          "--node-group-port", String.valueOf(groupPort),
          "--node-log-dir", "build/logs-" + port,
          "--node-backup-dir", "build/backup-" + port,
          "--node-metadata-dir", "build/metadata-" + port,
          "--node-config-dir", "build/config-" + port)
      );
    }

    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Started the server in diagnostic mode", "Started the server in diagnostic mode")
    ));
  }

  @Test
  public void testSingleStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.main("attach", "-d", "127.0.0.1:" + ports[0], "-s", "127.0.0.1:" + ports[2]);
    waitedAssert(out::getLog, containsString("Command successful"));

    out.clearLog();
    ConfigTool.main("activate", "-s", "127.0.0.1:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, containsString("Moved to State[ ACTIVE-COORDINATOR ]"));
    waitedAssert(out::getLog, containsString("Moved to State[ PASSIVE-STANDBY ]"));
    waitedAssert(out::getLog, containsString("Command successful"));

    createDatasetAndPerformAssertions(ports);
  }

  @Test
  public void testMultiStripeActivation() throws Exception {
    int[] ports = this.ports.getPorts();
    ConfigTool.main("attach", "-t", "stripe", "-d", "127.0.0.1:" + ports[0], "-s", "127.0.0.1:" + ports[2]);
    waitedAssert(out::getLog, containsString("Command successful"));

    out.clearLog();
    ConfigTool.main("activate", "-s", "127.0.0.1:" + ports[0], "-n", "tc-cluster", "-l", licensePath().toString());
    waitedAssert(out::getLog, stringContainsInOrder(
        Arrays.asList("Moved to State[ ACTIVE-COORDINATOR ]", "Moved to State[ ACTIVE-COORDINATOR ]")
    ));
    waitedAssert(out::getLog, containsString("Command successful"));

    createDatasetAndPerformAssertions(ports);
  }

  private void createDatasetAndPerformAssertions(int[] ports) throws Exception {
    Set<InetSocketAddress> node = Collections.singleton(InetSocketAddress.createUnresolved("127.0.0.1", ports[0]));
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
