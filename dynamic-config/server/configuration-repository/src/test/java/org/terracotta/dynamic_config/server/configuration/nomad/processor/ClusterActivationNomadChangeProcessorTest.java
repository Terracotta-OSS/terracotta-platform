/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.dynamic_config.server.configuration.nomad.processor;

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
    processor = new ClusterActivationNomadChangeProcessor(1, "foo");
  }

  @Test
  public void testGetConfigWithChange() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(topology.getCluster());

    processor.validate(null, change);

    assertThat(change.apply(null), notNullValue());
  }

  @Test
  public void testCanApplyWithNonNullBaseConfig() throws Exception {
    ClusterActivationNomadChange change = new ClusterActivationNomadChange(new Cluster("cluster"));
    NodeContext topology = new NodeContext(new Cluster(new Stripe(Node.newDefaultNode("foo", "localhost"))), 1, "foo");

    expectedException.expect(NomadException.class);
    expectedException.expectMessage("Existing config must be null. Found: " + topology);

    processor.validate(topology, change);
  }
}