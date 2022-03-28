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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.hasExitStatus;

@ClusterDefinition
public class GetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void testNode_getOneOffheap_unknownOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.blah"),
        allOf(
            not(hasExitStatus(0)),
            containsOutput("No configuration found for: offheap-resources.blah")));
  }

  @Test
  public void testNode_getOneOffheap() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testNode_getTwoOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(
            containsOutput("offheap-resources.main=512MB"),
            containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testNode_getAllOffheaps() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testNode_getAllDataDirs() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs"),
        containsOutput("stripe.1.node.1.data-dirs=main:node-1-1" + separator + "data-dir"));
  }

  @Test
  public void testNode_getClientReconnectWindow() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=120s"));
  }

  @Test
  public void testNode_getNodePort() {
    assertThat(
        configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-port"),
        containsOutput("stripe.1.node.1.node-port=" + getNodePort()));
  }
}
