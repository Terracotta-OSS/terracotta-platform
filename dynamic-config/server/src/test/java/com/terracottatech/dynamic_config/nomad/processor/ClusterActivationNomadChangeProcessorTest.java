/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.ConfigControllerImpl;
import com.terracottatech.nomad.server.NomadException;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

public class ClusterActivationNomadChangeProcessorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private static final String NODE_NAME = "node-1";
  private static final String STRIPE_NAME = "stripe-1";

  private ClusterActivationNomadChangeProcessor processor;

  @Before
  public void setUp() {
    ConfigController configController = new ConfigControllerImpl(() -> NODE_NAME, () -> STRIPE_NAME);
    processor = new ClusterActivationNomadChangeProcessor(configController);
  }

  @Test
  public void testGetConfigWithChange() throws Exception {
    Node node = new Node();

    node.setNodeName(NODE_NAME);
    node.setNodeHostname("localhost");
    node.setNodePort(3000);
    node.setNodeLogDir(temporaryFolder.newFolder().toPath());
    node.setClientReconnectWindow(120, TimeUnit.SECONDS);

    Stripe stripe = new Stripe(Collections.singletonList(node));
    Cluster cluster = new Cluster(Collections.singletonList(stripe));

    ClusterActivationNomadChange change = new ClusterActivationNomadChange("cluster", cluster);

    String configWithChange = processor.getConfigWithChange(null, change);

    assertThat(configWithChange, notNullValue());
  }

  @Test
  public void testCanApplyWithNonNullBaseConfig() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange("cluster", new Cluster());

    expectedException.expect(NomadException.class);
    expectedException.expectMessage("must be null");

    processor.getConfigWithChange("baseConfig", change);
  }
}