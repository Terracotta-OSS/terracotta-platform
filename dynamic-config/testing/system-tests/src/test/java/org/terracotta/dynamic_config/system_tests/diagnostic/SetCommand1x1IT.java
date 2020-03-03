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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;

import static java.io.File.separator;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.hasExitStatus;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

@ClusterDefinition
public class SetCommand1x1IT extends DynamicConfigIT {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  /*<--Single Node Tests-->*/
  @Test
  public void setOffheapResource() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=512MB"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=512MB")));
  }

  @Test
  public void setTcProperties() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something=value"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.tc-properties.something"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.tc-properties.something=value")));
  }

  @Test
  public void setClientReconnectWindow() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window=10s"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "client-reconnect-window"),
        allOf(hasExitStatus(0), containsOutput("client-reconnect-window=10s")));
  }

  @Test
  public void setSecurityAuthc() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-authc=file"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "security-authc"),
        allOf(hasExitStatus(0), containsOutput("security-authc=file")));
  }

  @Test
  public void setNodeGroupPort() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-group-port=9630"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-group-port"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.node-group-port=9630")));
  }

  @Test
  public void setSecurityWhitelist() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "security-dir=/path/to/security/dir", "-c", "security-whitelist=true"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "security-whitelist"),
        allOf(hasExitStatus(0), containsOutput("security-whitelist=true")));
  }

  @Test
  public void setDataDir() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main=user-data/main/stripe1-node1-data-dir"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.data-dirs.main"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.data-dirs.main=user-data" + separator + "main" + separator + "stripe1-node1-data-dir")));
  }

  @Test
  public void setNodeBackupDir() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-backup-dir=backup/stripe1-node1-backup"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "stripe.1.node.1.node-backup-dir"),
        allOf(hasExitStatus(0), containsOutput("stripe.1.node.1.node-backup-dir=backup" + separator + "stripe1-node1-backup")));
  }

  @Test
  public void setTwoProperties() {
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main=1GB", "-c", "stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir"), is(successful()));

    assertThat(configToolInvocation("get", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources.main", "-c", "stripe.1.node.1.data-dirs.main"),
        allOf(hasExitStatus(0), containsOutput("offheap-resources.main=1GB"), containsOutput("stripe.1.node.1.data-dirs.main=stripe1-node1-data-dir")));
  }
}
