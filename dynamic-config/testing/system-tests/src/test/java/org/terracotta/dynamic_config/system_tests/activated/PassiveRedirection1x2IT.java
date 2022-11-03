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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2)
public class PassiveRedirection1x2IT extends DynamicConfigIT {

  @BeforeClass
  public static void beforeClass() throws Exception {
    assumeThat(System.getProperty("java.version"), not(startsWith("1.8")));
  }

  @Override
  protected String getDefaultHostname(int stripeId, int nodeId) {
    return "testhostname";
  }

  @Test
  public void passiveRedirectsToInternalAddress() throws ConnectionException, IOException {
    attachAll();
    activateCluster();

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE"),
        is(successful()));

    int passiveId = waitForNPassives(1, 1)[0];
    int activeId = waitForActive(1);

    // Redirecting client: /127.0.0.1:58937 to proposed address: C02YJ2F2JGH6.local:46238
    try (Connection connection = ConnectionFactory.connect(singletonList(getNodeAddress(1, passiveId)), new Properties())) {
      assertTrue(connection.isValid());
      waitUntilServerLogs(getNode(1, passiveId), "Redirecting client: ");
      waitUntilServerLogs(getNode(1, passiveId), " to proposed address: " + getNodeHostPort(1, activeId)); // localhost:port (== <hostname>:<bind-port>)
    }
  }

  @Test
  public void passiveRedirectsToPublicAddress() throws IOException, ConnectionException {
    attachAll();
    activateCluster();

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(), "-c", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE"),
        is(successful()));

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.public-hostname=localhost",
            "-c", "stripe.1.node.1.public-port=" + getNodePort(1, 1),
            "-c", "stripe.1.node.2.public-hostname=localhost",
            "-c", "stripe.1.node.2.public-port=" + getNodePort(1, 2)
        ),
        is(successful()));

    int passiveId = waitForNPassives(1, 1)[0];
    int activeId = waitForActive(1);

    // Redirecting client: /127.0.0.1:59121 to node: node-1-1@localhost:42894 through public endpoint
    try (Connection connection = ConnectionFactory.connect(singletonList(InetSocketAddress.createUnresolved(getDefaultHostname(1, passiveId), getNodePort(1, passiveId))), new Properties())) {
      assertTrue(connection.isValid());
      waitUntilServerLogs(getNode(1, passiveId), "Redirecting client: ");
      waitUntilServerLogs(getNode(1, passiveId), " to node: " + getNodeName(1, activeId) + "@localhost:" + getNodePort(1, activeId) + " through public endpoint");
    }
  }

  @Test
  public void passiveRedirectsToBindAddress() throws IOException, ConnectionException {
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(1, 1), "-auto-restart",
            "-setting", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE",
            "-setting", "bind-address=127.0.0.1"
        ),
        is(successful()));
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(1, 2), "-auto-restart",
            "-setting", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE",
            "-setting", "bind-address=127.0.0.1"
        ),
        is(successful()));

    attachAll();
    activateCluster();
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    // Redirecting client: /127.0.0.1:59053 to proposed address: 127.0.0.1:37132
    try (Connection connection = ConnectionFactory.connect(singletonList(InetSocketAddress.createUnresolved(getDefaultHostname(1, passiveId), getNodePort(1, passiveId))), new Properties())) {
      assertTrue(connection.isValid());
      waitUntilServerLogs(getNode(1, passiveId), "Redirecting client: ");
      waitUntilServerLogs(getNode(1, passiveId), " to proposed address: 127.0.0.1:" + getNodePort(1, activeId)); // 127.0.0.1:port (== <bind-addr>:<bind-port>)
    }
  }

  @Test
  public void passiveRedirectsToBindAddressWithPublicEndpoint() throws IOException, ConnectionException {
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(1, 1), "-auto-restart",
            "-setting", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE",
            "-setting", "bind-address=127.0.0.1"
        ),
        is(successful()));
    assertThat(
        configTool("set", "-connect-to", "localhost:" + getNodePort(1, 2), "-auto-restart",
            "-setting", "logger-overrides=org.terracotta.dynamic_config.server.service.DynamicConfigNetworkTranslator:TRACE",
            "-setting", "bind-address=127.0.0.1"
        ),
        is(successful()));

    attachAll();
    activateCluster();
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    assertThat(
        configTool("set", "-s", "localhost:" + getNodePort(),
            "-c", "stripe.1.node.1.public-hostname=localhost",
            "-c", "stripe.1.node.1.public-port=" + getNodePort(1, 1),
            "-c", "stripe.1.node.2.public-hostname=localhost",
            "-c", "stripe.1.node.2.public-port=" + getNodePort(1, 2)
        ),
        is(successful()));

    // Redirecting client: /127.0.0.1:59447 to node: node-1-2@localhost:46384 through public endpoint
    try (Connection connection = ConnectionFactory.connect(singletonList(InetSocketAddress.createUnresolved(getDefaultHostname(1, passiveId), getNodePort(1, passiveId))), new Properties())) {
      assertTrue(connection.isValid());
      waitUntilServerLogs(getNode(1, passiveId), "Redirecting client: ");
      waitUntilServerLogs(getNode(1, passiveId), " to node: " + getNodeName(1, activeId) + "@localhost:" + getNodePort(1, activeId) + " through public endpoint");
    }
  }
}
