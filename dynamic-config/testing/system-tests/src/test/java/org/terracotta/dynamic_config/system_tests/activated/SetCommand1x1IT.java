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

import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static java.io.File.separator;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition(autoActivate = true)
public class SetCommand1x1IT extends DynamicConfigIT {
  @Test
  public void setOffheapResource_decreaseSize() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1MB"),
        allOf(not(hasExitStatus(0)), containsOutput("should be larger than the old size")));
  }

  @Test
  public void setOffheapResource_increaseSize() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=1GB")));
  }

  @Test
  public void setOffheapResource_addResource() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=second:1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.second=1GB")));
  }

  @Test
  public void setOffheapResources_addResources() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.third=1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second", "-c", "offheap-resources.third"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.second=1GB"), containsOutput("offheap-resources.third=1GB")));
  }

  @Test
  public void setOffheapResources_addResource_increaseSize() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=main:1GB,second:1GB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources=foo:1GB,main:1GB,second:1GB")));
  }

  @Test
  public void setOffheapResources_newResource_decreaseSize() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.second=1GB", "-c", "offheap-resources.main=1MB"),
        allOf(not(hasExitStatus(0)), containsOutput("should be larger than the old size")));
  }

  @Test
  public void setDataDir_updatePath() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.main=user-data/main/stripe1-node1-data-dir"),
        allOf(not(hasExitStatus(0)), containsOutput("A data directory with name: main already exists")));
  }

  @Test
  public void setDataDir_overlappingPaths() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.first=terracotta1-1/data-dir"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-1"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_overLappingPaths_flavor2() {
    assertThat(
        configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-1"),
        allOf(not(hasExitStatus(0)), containsOutput("overlaps with the existing data directory")));
  }

  @Test
  public void setDataDir_addOneNonExistentDataDir() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1"));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs=second:user-data/main/stripe1-node1-data-dir-1,third:user-data/main/stripe1-node1-data-dir-2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2")));
  }

  @Test
  public void setDataDir_addMultipleNonExistentDataDirs_flavor2() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second=user-data/main/stripe1-node1-data-dir-1", "-c", "data-dirs.third=user-data/main/stripe1-node1-data-dir-2"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.second"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.second=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-1")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "data-dirs.third"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.third=user-data" + separator + "main" + separator + "stripe1-node1-data-dir-2")));
  }

  @Test
  public void setFailover_Priority_Consistency() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "failover-priority=consistency:2"),
        allOf(hasExitStatus(0), containsOutput("restart of the cluster is required")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "failover-priority"),
        allOf(hasExitStatus(0), containsOutput("failover-priority=consistency:2")));
  }

  @Test
  public void setNodeLogDir_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir=logs/stripe1"),
        containsOutput("restart of the cluster is required"));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-log-dir"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.node-log-dir=logs" + separator + "stripe1")));
  }

  @Test
  public void setNodeBindAddress_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address=127.0.0.1"),
        containsOutput("restart of the cluster is required"));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-bind-address"),
        containsOutput("stripe.1.node.1.node-bind-address=127.0.0.1"));
  }

  @Test
  public void setNodeGroupBindAddress_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address=127.0.0.1"),
        allOf(hasExitStatus(0), containsOutput("restart of the cluster is required")));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-group-bind-address"),
        containsOutput("stripe.1.node.1.node-group-bind-address=127.0.0.1"));
  }

  @Test
  public void testTcProperty_postActivation() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo=bar"),
        allOf(hasExitStatus(0), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));

    assertThat(configToolInvocation("get", "-r", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), not(containsOutput("tc-properties=foo:bar"))));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), containsOutput("tc-properties=foo:bar")));

    assertThat(configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "tc-properties.foo"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "tc-properties"),
        allOf(hasExitStatus(0), not(containsOutput("tc-properties=foo:bar"))));
  }

  @Test
  public void testSetLogger() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides=org.terracotta:TRACE,com.tc:TRACE"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides"),
        allOf(hasExitStatus(0), containsOutput("node-logger-overrides=com.tc:TRACE,org.terracotta:TRACE")));

    assertThat(configToolInvocation("unset", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-logger-overrides.com.tc"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "node-logger-overrides"),
        allOf(hasExitStatus(0), containsOutput("node-logger-overrides=org.terracotta:TRACE")));
  }
}
