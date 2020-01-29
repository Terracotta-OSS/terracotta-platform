/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.processor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.nomad.server.NomadException;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class ClusterActivationNomadChangeProcessorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private ClusterActivationNomadChangeProcessor processor;
  private NodeContext topology = new NodeContext(new Cluster("bar", new Stripe(Node.newDefaultNode("foo", "localhost"))), 1, "foo");

  @Before
  public void setUp() {
    processor = new ClusterActivationNomadChangeProcessor(1, "foo", topology.getCluster());
  }

  @Test
  public void testGetConfigWithChange() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(topology.getCluster());

    NodeContext configWithChange = processor.tryApply(null, change);

    assertThat(configWithChange, notNullValue());
  }

  @Test
  public void testCanApplyWithNonNullBaseConfig() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(new Cluster("cluster"));
    NodeContext topology = new NodeContext(new Cluster(new Stripe(Node.newDefaultNode("foo", "localhost"))), 1, "foo");

    expectedException.expect(NomadException.class);
    expectedException.expectMessage("Existing config must be null. Found: " + topology);

    processor.tryApply(topology, change);
  }
}