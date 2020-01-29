/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

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
    NodeContext nodeContext = new NodeContext(new Cluster(new Stripe(node)), 1, "foo");

    String hash = hashComputer.computeHash(nodeContext);
    String json = om.writeValueAsString(nodeContext);
    NodeContext reloaded = om.readValue(json, NodeContext.class);
    String reloadedHash = hashComputer.computeHash(reloaded);

    assertThat(reloaded, is(nodeContext));
    assertThat(reloadedHash, is(hash));
  }
}
