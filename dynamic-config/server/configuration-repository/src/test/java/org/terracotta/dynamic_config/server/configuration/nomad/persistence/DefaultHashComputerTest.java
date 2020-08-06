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
import org.junit.Test;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.json.ObjectMapperFactory;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
public class DefaultHashComputerTest {
  @Test
  public void computeHash() throws IOException {
    ObjectMapper om = new ObjectMapperFactory().pretty().withModule(new DynamicConfigApiJsonModule()).create();
    HashComputer<NodeContext> hashComputer = new DefaultHashComputer(om);

    Node node = Testing.newTestNode("foo", "localhost");
    NodeContext nodeContext = new NodeContext(Testing.newTestCluster(new Stripe().addNodes(node)), 1, "foo");

    String hash = hashComputer.computeHash(nodeContext);
    String json = om.writeValueAsString(nodeContext);
    NodeContext reloaded = om.readValue(json, NodeContext.class);
    String reloadedHash = hashComputer.computeHash(reloaded);

    assertThat(reloaded, is(nodeContext));
    assertThat(reloadedHash, is(hash));
  }
}
