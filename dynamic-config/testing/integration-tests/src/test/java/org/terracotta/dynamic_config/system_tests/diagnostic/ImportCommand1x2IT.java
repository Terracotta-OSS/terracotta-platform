/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, failoverPriority = "")
public class ImportCommand1x2IT extends DynamicConfigIT {

  @Test
  public void test_relay_import() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay.properties");
    assertThat(configTool("import", "-f", configFile.toString()), is(successful()));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay", "-c", "replica-hostname", "-c", "replica-port", "-t", "index"),
      allOf(is(successful()), containsOutput("stripe.1.node.2.relay=true"), containsOutput("stripe.1.node.2.replica-hostname=localhost"), containsOutput("stripe.1.node.2.replica-port=" + 1234)));
  }

  @Test
  public void test_relay_invalid_missing_property() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-invalid1.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(is(not(successful())),
        containsOutput("The relay setting is enabled for node with name: node-1-2, relay properties: {replica-hostname=localhost, replica-port=null} aren't well-formed")));
  }

  @Test
  public void test_relay_disabled_invalid_partial_config() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-invalid3.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(is(not(successful())),
        containsOutput("The relay setting is disabled for node with name: node-1-1, properties: {replica-hostname=null, replica-port=1234} are partially configured")));
  }

  @Test
  public void test_relay_invalid_mutual_exclusion() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-invalid2.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(is(not(successful())),
        containsOutput("Node with name: node-1-2 has the replica setting enabled and cannot coexist with other nodes with names: [node-1-1]")));
  }

  @Test
  public void test_failed_import_with_replica_properties_on_normal_node() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x1-replica.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(not(successful()), containsOutput("Node with name: node-1-1 has the replica setting enabled. IMPORT operation is not supported on replica node")));
  }

  @Test
  public void test_failed_import_on_replica_node() throws Exception {
    stopNode(1, 1);
    startNode(1, 1, getNewOptions(getNode(1, 1), "-replica", "true", "-relay-hostname", "localhost", "-relay-port", "1234", "-relay-group-port", "5678"));
    waitForPassiveRelay(1, 1);
    Path configFile = copyConfigProperty("/config-property-files/1x1-relay.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(not(successful()), containsOutput("Node: " + getNodeHostPort(1, 1) +  " has the replica setting enabled")));
  }
}
