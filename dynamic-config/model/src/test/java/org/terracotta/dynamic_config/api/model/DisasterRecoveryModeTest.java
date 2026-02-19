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
package org.terracotta.dynamic_config.api.model;

import org.junit.Test;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;

public class DisasterRecoveryModeTest {

  @Test
  public void test_fromNode_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    DisasterRecoveryMode mode = DisasterRecoveryMode.fromNode(relayNode);
    assertThat(mode, is(equalTo(DisasterRecoveryMode.RELAY)));
    assertThat(mode.isEnabled(relayNode), is(true));
  }

  @Test
  public void test_fromNode_none() {
    Node normalNode = newTestNode("normal", "localhost");

    DisasterRecoveryMode mode = DisasterRecoveryMode.fromNode(normalNode);
    assertThat(mode, is(equalTo(DisasterRecoveryMode.NONE)));
    assertThat(mode.isEnabled(normalNode), is(true));
  }

  @Test
  public void test_relay_mode_getPeer() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    InetSocketAddress peer = DisasterRecoveryMode.RELAY.getPeer(relayNode).get();
    assertThat(peer.getHostString(), is(equalTo("replica-host")));
    assertThat(peer.getPort(), is(equalTo(9410)));
  }

  @Test
  public void test_none_getPeer_when_disabled() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.RELAY.getPeer(normalNode), is(Optional.empty()));
  }

  @Test
  public void test_none_getPeer_returns_null() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.NONE.getPeer(normalNode), is(Optional.empty()));
  }

  @Test
  public void test_getRequiredProperties_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    Map<String, OptionalConfig<?>> requiredProperties = DisasterRecoveryMode.RELAY.getRequiredProperties(relayNode);
    assertThat(requiredProperties.size(), is(equalTo(2)));
    assertThat(requiredProperties.containsKey(SettingName.REPLICA_HOSTNAME), is(true));
    assertThat(requiredProperties.containsKey(SettingName.REPLICA_PORT), is(true));
  }

  @Test
  public void test_getRequiredProperties_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.NONE.getRequiredProperties(normalNode).size(), is(equalTo(0)));
  }

  @Test
  public void test_isEnabled_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost").setRelayMode(true);
    assertThat(DisasterRecoveryMode.RELAY.isEnabled(relayNode), is(true));
    assertThat(DisasterRecoveryMode.REPLICA.isEnabled(relayNode), is(false));
    assertThat(DisasterRecoveryMode.NONE.isEnabled(relayNode), is(false));
  }

  @Test
  public void test_isEnabled_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.RELAY.isEnabled(normalNode), is(false));
    assertThat(DisasterRecoveryMode.REPLICA.isEnabled(normalNode), is(false));
    assertThat(DisasterRecoveryMode.NONE.isEnabled(normalNode), is(true));
  }
}
