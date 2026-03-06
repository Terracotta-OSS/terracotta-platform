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
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class DisasterRecoveryModeTest {

  @Test
  public void test_fromNode_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelay(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    DisasterRecoveryMode mode = DisasterRecoveryMode.fromNode(relayNode);
    assertThat(mode, is(equalTo(DisasterRecoveryMode.RELAY)));
    assertThat(mode.isEnabled(relayNode), is(true));
  }

  @Test
  public void test_fromNode_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplica(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    DisasterRecoveryMode mode = DisasterRecoveryMode.fromNode(replicaNode);
    assertThat(mode, is(equalTo(DisasterRecoveryMode.REPLICA)));
    assertThat(mode.isEnabled(replicaNode), is(true));
  }

  @Test
  public void test_fromNode_none() {
    Node normalNode = newTestNode("normal", "localhost");

    DisasterRecoveryMode mode = DisasterRecoveryMode.fromNode(normalNode);
    assertThat(mode, is(equalTo(DisasterRecoveryMode.NONE)));
    assertThat(mode.isEnabled(normalNode), is(true));
  }

  @Test
  public void test_fromNode_both_modes_throws_exception() {
    Node invalidNode = newTestNode("invalid", "localhost")
      .setRelay(true)
      .setReplica(true);

    assertThat(
      () -> DisasterRecoveryMode.fromNode(invalidNode),
      is(throwing(instanceOf(AssertionError.class))
        .andMessage(containsString("has both relay and replica settings enabled"))));
  }

  @Test
  public void test_relay_mode_getPeer() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelay(true)
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
  public void test_replica_mode_getPeer() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplica(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    InetSocketAddress peer = DisasterRecoveryMode.REPLICA.getPeer(replicaNode).get();
    assertThat(peer.getHostString(), is(equalTo("relay-host")));
    assertThat(peer.getPort(), is(equalTo(9410)));
  }

  @Test
  public void test_relay_mode_getPeerGroupPort() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplica(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    assertThat(DisasterRecoveryMode.RELAY.getPeerGroupPort(replicaNode), is(Optional.empty()));
  }

  @Test
  public void test_replica_mode_getPeerGroupPort() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplica(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    InetSocketAddress peerGroupPort = DisasterRecoveryMode.REPLICA.getPeerGroupPort(replicaNode).get();
    assertThat(peerGroupPort.getHostString(), is(equalTo("relay-host")));
    assertThat(peerGroupPort.getPort(), is(equalTo(9430)));
  }

  @Test
  public void test_replica_mode_getPeerGroupPort_when_disabled() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.REPLICA.getPeerGroupPort(normalNode), is(Optional.empty()));
  }

  @Test
  public void test_none_getPeer_returns_null() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.NONE.getPeer(normalNode), is(Optional.empty()));
  }

  @Test
  public void test_none_getPeerGroupPort_returns_null() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.NONE.getPeerGroupPort(normalNode), is(Optional.empty()));
  }

  @Test
  public void test_getRequiredProperties_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelay(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    Map<String, OptionalConfig<?>> requiredProperties = DisasterRecoveryMode.RELAY.getRequiredProperties(relayNode);
    assertThat(requiredProperties.size(), is(equalTo(2)));
    assertThat(requiredProperties.get(SettingName.REPLICA_HOSTNAME), is(OptionalConfig.of(Setting.REPLICA_HOSTNAME, "replica-host")));
    assertThat(requiredProperties.get(SettingName.REPLICA_PORT), is(OptionalConfig.of(Setting.REPLICA_PORT, 9410)));
  }

  @Test
  public void test_getRequiredProperties_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplica(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    Map<String, OptionalConfig<?>> requiredProperties = DisasterRecoveryMode.REPLICA.getRequiredProperties(replicaNode);
    assertThat(requiredProperties.size(), is(equalTo(3)));
    assertThat(requiredProperties.get(SettingName.RELAY_HOSTNAME), is(OptionalConfig.of(Setting.RELAY_HOSTNAME, "relay-host")));
    assertThat(requiredProperties.get(SettingName.RELAY_PORT), is(OptionalConfig.of(Setting.RELAY_PORT, 9410)));
    assertThat(requiredProperties.get(SettingName.RELAY_GROUP_PORT), is(OptionalConfig.of(Setting.RELAY_GROUP_PORT, 9430)));
  }

  @Test
  public void test_getRequiredProperties_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.NONE.getRequiredProperties(normalNode).size(), is(equalTo(0)));
  }

  @Test
  public void test_isEnabled_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost").setRelay(true);
    assertThat(DisasterRecoveryMode.RELAY.isEnabled(relayNode), is(true));
    assertThat(DisasterRecoveryMode.REPLICA.isEnabled(relayNode), is(false));
    assertThat(DisasterRecoveryMode.NONE.isEnabled(relayNode), is(false));
  }

  @Test
  public void test_isEnabled_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost").setReplica(true);
    assertThat(DisasterRecoveryMode.RELAY.isEnabled(replicaNode), is(false));
    assertThat(DisasterRecoveryMode.REPLICA.isEnabled(replicaNode), is(true));
    assertThat(DisasterRecoveryMode.NONE.isEnabled(replicaNode), is(false));
  }

  @Test
  public void test_isEnabled_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DisasterRecoveryMode.RELAY.isEnabled(normalNode), is(false));
    assertThat(DisasterRecoveryMode.REPLICA.isEnabled(normalNode), is(false));
    assertThat(DisasterRecoveryMode.NONE.isEnabled(normalNode), is(true));
  }
}
