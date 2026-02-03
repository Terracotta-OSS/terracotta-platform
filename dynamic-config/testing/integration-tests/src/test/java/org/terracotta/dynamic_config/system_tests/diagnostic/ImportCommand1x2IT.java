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
  public void test_relay_source_import() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-source.properties");
    assertThat(configTool("import", "-f", configFile.toString()), is(successful()));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay-source-hostname", "-c", "relay-source-port", "-t", "index"),
      allOf(is(successful()), containsOutput("relay-source-hostname=localhost"), containsOutput("relay-source-port=" + 1234)));
  }

  @Test
  public void test_relay_source_invalid_missing_property() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-invalid1.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(is(not(successful())),
        containsOutput("Relay source properties: {relay-source-hostname=localhost, relay-source-port=null} of node with name: node-1-2 aren't well-formed.")));
  }

  @Test
  public void test_relay_source_invalid_mutual_exclusion() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x2-relay-invalid2.properties");
    assertThat(configTool("import", "-f", configFile.toString()),
      allOf(is(not(successful())),
        containsOutput("Cluster has both relay source and relay destination properties configured across different nodes. Nodes with relay source properties: [node-1-1]. Nodes with relay destination properties: [node-1-2]")));
  }

  @Test
  public void test_relay_destination_import() throws Exception {
    Path configFile = copyConfigProperty("/config-property-files/1x1-relay-destination.properties");
    assertThat(configTool("import", "-f", configFile.toString()), is(successful()));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "relay-destination-hostname", "-c", "relay-destination-port", "-c", "relay-destination-group-port", "-t", "index"),
      allOf(containsOutput("relay-destination-hostname=localhost"), containsOutput("relay-destination-port=" + 1234), containsOutput("relay-destination-group-port=" + 5678)));
  }
}
