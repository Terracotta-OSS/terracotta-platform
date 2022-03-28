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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.json.Json;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.Node.newDefaultNode;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputerTest {
  @Test
  public void computeHash() throws IOException {
    ObjectMapper om = Json.copyObjectMapper(true).configure(SerializationFeature.INDENT_OUTPUT, false);
    HashComputer<NodeContext> hashComputer = new DefaultHashComputer(Json.copyObjectMapper(true));

    Node node = newDefaultNode("foo", "localhost");
    NodeContext nodeContext = new NodeContext(Cluster.newDefaultCluster(new Stripe(node)), 1, "foo");

    String hash = hashComputer.computeHash(nodeContext);
    String json = om.writeValueAsString(nodeContext);
    NodeContext reloaded = om.readValue(json, NodeContext.class);
    String reloadedHash = hashComputer.computeHash(reloaded);

    assertThat(reloaded, is(nodeContext));
    assertThat(reloadedHash, is(hash));
  }
}
