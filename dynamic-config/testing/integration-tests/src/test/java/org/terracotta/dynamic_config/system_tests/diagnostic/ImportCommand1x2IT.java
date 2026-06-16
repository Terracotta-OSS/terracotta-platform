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
}
