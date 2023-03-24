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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.json.DynamicConfigModelJsonModule;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import java.util.function.BiConsumer;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class ClusterTest {

  @Mock BiConsumer<Integer, Node> consumer;

  Json json = new DefaultJsonFactory().withModule(new DynamicConfigModelJsonModule()).create();

  Node node1 = Testing.newTestNode("node1", "localhost", 9410)
      .putDataDir("data", RawPath.valueOf("data"))
      .setBackupDir(RawPath.valueOf("backup"))
      .setBindAddress("0.0.0.0")
      .setGroupBindAddress("0.0.0.0")
      .setGroupPort(9430)
      .setLogDir(RawPath.valueOf("log"))
      .setMetadataDir(RawPath.valueOf("metadata"))
      .setSecurityAuditLogDir(RawPath.valueOf("audit"));

  Node node2 = Testing.newTestNode("node2", "localhost", 9411, Testing.N_UIDS[2])
      .putDataDir("data", RawPath.valueOf("/data/cache2"));

  Stripe stripe1 = new Stripe().addNodes(node1);
  Cluster cluster = Testing.newTestCluster("c", stripe1)
      .setClientLeaseDuration(1, TimeUnit.SECONDS)
      .setClientReconnectWindow(2, TimeUnit.MINUTES)
      .setFailoverPriority(availability())
      .setSecurityAuthc("ldap")
      .setSecuritySslTls(true)
      .setSecurityWhitelist(true);

  @Test
  public void test_isEmpty() {
    assertFalse(cluster.isEmpty());
    assertTrue(new Cluster().isEmpty());
    assertTrue(new Cluster(new Stripe()).isEmpty());
  }

  @Test
  public void test_containsNode() {
    assertTrue(cluster.containsNode(node1.getUID()));
    assertFalse(cluster.containsNode(node2.getUID()));
  }

  @Test
  public void test_clone() {
    assertThat(cluster.clone(), is(equalTo(cluster)));
    assertThat(json.map(cluster.clone()), is(equalTo(json.map(cluster))));
  }

  @Test
  public void test_getNode() {
    assertThat(cluster.getNode(node1.getUID()).get(), is(equalTo(node1)));
    assertFalse(cluster.getNode(node2.getUID()).isPresent());
  }

  @Test
  public void test_detach_node() {
    cluster.removeNode(node2.getUID());
    assertFalse(cluster.isEmpty());
    assertThat(cluster.getStripes(), hasSize(1));

    cluster.removeNode(node1.getUID());
    assertTrue(cluster.isEmpty());
    assertThat(cluster.getStripes(), hasSize(0));
  }

  @Test
  public void test_detach_stripe() {
    cluster.removeStripe(stripe1);
    assertTrue(cluster.isEmpty());
    assertThat(cluster.getStripes(), hasSize(0));
  }

  @Test
  public void test_getSingleNode() {
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    stripe1.addNode(node2);
    assertThat(() -> cluster.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    stripe1.removeNode(node2.getUID());
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    cluster.addStripe(new Stripe().addNodes(node2));
    assertThat(() -> cluster.getSingleNode(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    cluster.removeNode(node2.getUID());
    assertThat(cluster.getSingleNode().get(), is(sameInstance(node1)));

    // empty
    stripe1.removeNode(node1.getUID());
    assertThat(cluster.getSingleNode().isPresent(), is(false));
  }

  @Test
  public void test_getSingleStripe() {
    assertThat(cluster.getSingleStripe().get(), is(sameInstance(stripe1)));

    Stripe stripe2 = new Stripe().addNodes(node2);
    cluster.addStripe(stripe2);
    assertThat(() -> cluster.getSingleStripe(), is(throwing(instanceOf(IllegalStateException.class))));

    // back to normal
    cluster.removeStripe(cluster.getStripes().get(1));
    assertThat(cluster.getSingleStripe().get(), is(sameInstance(stripe1)));

    // empty
    cluster.removeStripe(cluster.getStripes().get(0));
    assertThat(cluster.getSingleStripe().isPresent(), is(false));
  }

  @Test
  public void test_getStripeCount() {
    assertThat(cluster.getStripeCount(), is(equalTo(1)));
  }

  @Test
  public void test_getNodeCount() {
    assertThat(cluster.getNodeCount(), is(equalTo(1)));
  }

  @Test
  public void test_clusterEqualityWithDefaultValues() {
    // New cluster with no values set
    Cluster cluster = new Cluster();
    Cluster workingCluster = cluster.clone();
    assertTrue(cluster.equals(workingCluster));

    // get the value for a setting which possesses a default value and explicitly set that same value in the working cluster
    int defaultReconnectWindow = cluster.getClientReconnectWindow().orDefault().getExactQuantity(TimeUnit.SECONDS).intValueExact();
    workingCluster.setClientReconnectWindow(Measure.of(defaultReconnectWindow, TimeUnit.SECONDS));
    int updatedReconnectWindow = workingCluster.getClientReconnectWindow().get().getExactQuantity(TimeUnit.SECONDS).intValueExact();
    assertEquals(updatedReconnectWindow, defaultReconnectWindow);
    assertFalse(cluster.equals(workingCluster));

    // remove the value
    workingCluster.setClientReconnectWindow(null);
    assertTrue(cluster.equals(workingCluster));

    // repeat with a boolean default setting
    boolean defaultWhitelist = cluster.getSecurityWhitelist().orDefault().booleanValue();
    workingCluster.setSecurityWhitelist(defaultWhitelist);
    boolean updatedWhitelist = workingCluster.getSecurityWhitelist().get();
    assertEquals(updatedWhitelist, defaultWhitelist);
    assertFalse(cluster.equals(workingCluster));

    // remove the value
    workingCluster.setSecurityWhitelist(null);
    assertTrue(cluster.equals(workingCluster));
  }

  @Test
  public void test_clusterEqualityWithNonDefaultValues() {
    // New cluster with no values set
    Cluster cluster = new Cluster();
    Cluster workingCluster = cluster.clone();
    assertTrue(cluster.equals(workingCluster));

    // set the value for a setting which does not possess a default value
    String defaultAuthentication = cluster.getSecurityAuthc().orDefault();
    assertNull(defaultAuthentication);
    workingCluster.setSecurityAuthc(null);
    assertTrue(cluster.equals(workingCluster));

    cluster.setSecurityAuthc("availability");
    workingCluster.setSecurityAuthc("availability");
    assertTrue(cluster.equals(workingCluster));
  }
}