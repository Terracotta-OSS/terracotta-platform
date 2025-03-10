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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, failoverPriority = "")
public class GetCommand1x2IT extends DynamicConfigIT {

  @Before
  public void before() throws Exception {
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
  }

  @Test
  public void testStripe_getOneOffheap() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        containsOutput("offheap-resources.main=512MB"));
  }

  @Test
  public void testStripe_getTwoOffheaps() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "offheap-resources.foo"),
        allOf(containsOutput("offheap-resources.main=512MB"), containsOutput("offheap-resources.foo=1GB")));
  }

  @Test
  public void testStripe_getAllOffheaps() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        containsOutput("offheap-resources=foo:1GB,main:512MB"));
  }

  @Test
  public void testStripe_getAllDataDirs() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs", "-t", "index"),
        allOf(
            containsOutput("stripe.1.node.1.data-dirs=main:"),
            containsOutput("stripe.1.node.2.data-dirs=main:")));
  }

  @Test
  public void testStripe_getAllNodeHostnames() {
    assertThat(
        configTool("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.hostname", "-t", "index"),
        allOf(
            containsOutput("stripe.1.node.1.hostname=localhost"),
            containsOutput("stripe.1.node.2.hostname=localhost")));
  }
}
