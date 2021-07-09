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

import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.not;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;

@ClusterDefinition(failoverPriority = "")
public class GetCommand1x1IT extends DynamicConfigIT {

  @Test
  @Ignore("activate only manually to test verbose mode because it will otherwise change the whole logging system of the JVM running tests")
  public void ensureVerboseWorks() {
    assertThat(
        configTool("-verbose", "get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.blah"),
        not(containsOutput("offheap-resources.blah=")));
  }

  @Test
  public void testNode_getOneOffheap_unknownOffheap() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.blah"),
        not(containsOutput("offheap-resources.blah=")));
  }

  @Test
  public void testNode_getOneOffheap() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testNode_getTwoOffheaps() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(
            containsOutput("offheap-resources.main=512MB"),
            containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testNode_getAllOffheaps() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testNode_getAllDataDirs() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs", "-t", "index"),
        containsOutput("stripe.1.node.1.data-dirs=main:data-dir"));
  }

  @Test
  public void testNode_getClientReconnectWindow() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        containsOutput("client-reconnect-window=20s"));
  }

  @Test
  public void testNode_getNodePort() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.port", "-t", "index"),
        containsOutput("stripe.1.node.1.port=" + getNodePort()));
  }

  @Test
  public void test_getUID() {
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "cluster-uid"), containsOutput("cluster-uid="));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "node-uid"), containsOutput("node-uid="));
    assertThat(configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe-uid"), containsOutput("stripe-uid="));
  }
}
