/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.nomad.server.NomadException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertThrows;
import static org.junit.internal.matchers.ThrowableMessageMatcher.hasMessage;

@RunWith(MockitoJUnitRunner.class)
public class ClusterActivationNomadChangeProcessorTest {

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private ClusterActivationNomadChangeProcessor processor;
  private NodeContext topology = new NodeContext(
      Testing.newTestCluster("bar",
          Testing.newTestStripe("stripe-1").addNodes(
              Testing.newTestNode("foo", "localhost"))), Testing.N_UIDS[1]);

  @Before
  public void setUp() {
    processor = new ClusterActivationNomadChangeProcessor(Testing.N_UIDS[1]);
  }

  @Test
  public void testGetConfigWithChange() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(topology.getCluster());

    processor.validate(null, change);

    assertThat(change.apply(null), notNullValue());
  }

  @Test
  public void testCanApplyWithNonNullBaseConfig() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(Testing.newTestCluster("cluster"));
    NodeContext topology = new NodeContext(
        Testing.newTestCluster(
            Testing.newTestStripe("stripe-1").addNodes(
                Testing.newTestNode("foo", "localhost"))), Testing.N_UIDS[1]);

    NomadException e = assertThrows(NomadException.class, () -> processor.validate(topology, change));
    assertThat(e, hasMessage(equalTo("Found an existing configuration: " + topology)));
  }

  @Test
  public void testWithReplicaNodes() throws Exception {
    Node node1 = Testing.newTestNode("node1", "localhost4", Testing.N_UIDS[1])
      .setReplicaMode(true).setRelayHostname("localhost").setRelayPort(9410).setRelayGroupPort(9430);
    Node node2 = Testing.newTestNode("node2", "localhost5", Testing.N_UIDS[2]);
    Node node3 = Testing.newTestNode("node3", "localhost6", Testing.N_UIDS[3])
      .setReplicaMode(true).setRelayHostname("localhost").setRelayPort(9410).setRelayGroupPort(9430);
    Cluster cluster = Testing.newTestCluster("foo", Testing.newTestStripe("stripe1").addNodes(node1, node2),
      Testing.newTestStripe("stripe2").setUID(Testing.S_UIDS[2]).addNode(node3));
    NodeContext topology = new NodeContext(cluster, Testing.N_UIDS[1]);

    ClusterActivationNomadChange change = new ClusterActivationNomadChange(topology.getCluster());

    NomadException e = assertThrows(NomadException.class, () -> processor.validate(null, change));
    assertThat(e, hasMessage(equalTo("Nodes with names: [node1, node3] have replica-mode enabled. A cluster cannot be activated if replica-mode is enabled on any node.")));
  }
}
