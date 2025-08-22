/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.service;

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.STRIPE_NAME;
import static org.terracotta.dynamic_config.api.model.Testing.newTestCluster;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;

/**
 * @author Mathieu Carbou
 */
public class NameGeneratorTest {

  @Test
  public void test_stripe_dic_exhausted() throws IOException {
    Cluster cluster = newTestCluster("foo");
    try (Stream<String> lines = Files.lines(Paths.get("src/main/resources/dict/animals.txt"))) {
      lines
          .map(name -> newTestStripe(name, UID.newUID()))
          .forEach(cluster::addStripe);
    }
    cluster.addStripe(newTestStripe("stripe-1", UID.newUID()));
    cluster.addStripe(newTestStripe("stripe-2", UID.newUID()));

    Stripe newStripe = newTestStripe(STRIPE_NAME.getDefaultValue(), UID.newUID());
    cluster.addStripe(newStripe);
    NameGenerator.assignFriendlyNames(cluster, newStripe);

    assertThat(newStripe.getName(), is(equalTo("stripe-3")));
    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);
  }

  @Test
  public void test_node_dic_exhausted() throws IOException {
    Stripe stripe = newTestStripe("Canidae", UID.newUID());
    Cluster cluster = newTestCluster("foo", stripe);
    try (Stream<String> lines = Files.lines(Paths.get("src/main/resources/dict/greek.txt"))) {
      lines
          .map(name -> newTestNode(stripe.getName() + "-" + name, "hostname-" + UID.newUID(), UID.newUID()))
          .forEach(stripe::addNode);
    }
    stripe.addNode(newTestNode("Canidae-1", "hostname-" + UID.newUID(), UID.newUID()));
    stripe.addNode(newTestNode("Canidae-2", "hostname-" + UID.newUID(), UID.newUID()));

    Node newNode = newTestNode(NODE_NAME.getDefaultValue(), "hostname-" + UID.newUID(), UID.newUID());
    stripe.addNode(newNode);
    NameGenerator.assignFriendlyNodeName(cluster, stripe, newNode);

    assertThat(newNode.getName(), is(equalTo("Canidae-3")));
    new ClusterValidator(cluster).validate(ClusterState.ACTIVATED);
  }
}