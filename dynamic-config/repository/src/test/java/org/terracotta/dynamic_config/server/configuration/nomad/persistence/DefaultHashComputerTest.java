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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Test;
import org.terracotta.dynamic_config.api.json.DynamicConfigJsonModule;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputerTest {
  @Test
  public void computeHash() {
    Json om = new DefaultJsonFactory().withModule(new DynamicConfigJsonModule()).create();
    HashComputer hashComputer = new DefaultHashComputer();

    Node node = Testing.newTestNode("foo", "localhost");
    NodeContext nodeContext = new NodeContext(Testing.newTestCluster(Testing.newTestStripe("stripe-1").addNodes(node)), Testing.N_UIDS[1]);

    String hash = hashComputer.computeHash(new Config(nodeContext, Version.CURRENT));
    String json = om.toString(nodeContext);
    NodeContext reloaded = om.parse(json, NodeContext.class);
    String reloadedHash = hashComputer.computeHash(new Config(reloaded, Version.CURRENT));

    assertThat(reloaded, is(nodeContext));
    assertThat(reloadedHash, is(hash));
  }
}
