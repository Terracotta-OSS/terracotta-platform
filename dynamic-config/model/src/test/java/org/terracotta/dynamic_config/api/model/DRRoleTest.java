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
import org.terracotta.dynamic_config.api.service.MalformedClusterException;

import java.net.InetSocketAddress;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.terracotta.dynamic_config.api.model.Testing.newTestNode;
import static org.terracotta.testing.ExceptionMatcher.throwing;

public class DRRoleTest {

  @Test
  public void test_fromNode_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    DRRole role = DRRole.fromNode(relayNode);
    assertThat(role, is(equalTo(DRRole.RELAY_MODE)));
    assertThat(role.isEnabled(relayNode), is(true));
  }

  @Test
  public void test_fromNode_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    DRRole role = DRRole.fromNode(replicaNode);
    assertThat(role, is(equalTo(DRRole.REPLICA_MODE)));
    assertThat(role.isEnabled(replicaNode), is(true));
  }

  @Test
  public void test_fromNode_none() {
    Node normalNode = newTestNode("normal", "localhost");

    DRRole role = DRRole.fromNode(normalNode);
    assertThat(role, is(equalTo(DRRole.NONE)));
    assertThat(role.isEnabled(normalNode), is(true));
  }

  @Test
  public void test_fromNode_both_modes_throws_exception() {
    Node invalidNode = newTestNode("invalid", "localhost")
      .setRelayMode(true)
      .setReplicaMode(true);

    assertThat(
      () -> DRRole.fromNode(invalidNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("has both relay-mode and replica-mode enabled"))));
  }

  @Test
  public void test_relay_mode_getPeer() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    InetSocketAddress peer = DRRole.RELAY_MODE.getPeer(relayNode);
    assertThat(peer.getHostString(), is(equalTo("replica-host")));
    assertThat(peer.getPort(), is(equalTo(9410)));
  }

  @Test
  public void test_none_getPeer_when_disabled() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.RELAY_MODE.getPeer(normalNode), is(nullValue()));
  }

  @Test
  public void test_replica_mode_getPeer() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    InetSocketAddress peer = DRRole.REPLICA_MODE.getPeer(replicaNode);
    assertThat(peer.getHostString(), is(equalTo("relay-host")));
    assertThat(peer.getPort(), is(equalTo(9410)));
  }

  @Test
  public void test_replica_mode_getPeerGroupPort() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    InetSocketAddress peerGroupPort = DRRole.REPLICA_MODE.getPeerGroupPort(replicaNode);
    assertThat(peerGroupPort.getHostString(), is(equalTo("relay-host")));
    assertThat(peerGroupPort.getPort(), is(equalTo(9430)));
  }

  @Test
  public void test_replica_mode_getPeerGroupPort_when_disabled() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.REPLICA_MODE.getPeerGroupPort(normalNode), is(nullValue()));
  }

  @Test
  public void test_none_getPeer_returns_null() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.NONE.getPeer(normalNode), is(nullValue()));
  }

  @Test
  public void test_none_getPeerGroupPort_returns_null() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.NONE.getPeerGroupPort(normalNode), is(nullValue()));
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_valid() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    DRRole.RELAY_MODE.validateRequiredProperties(relayNode);
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_missing_hostname() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaPort(9410);

    assertThat(
      () -> DRRole.RELAY_MODE.validateRequiredProperties(relayNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("relay-mode is enabled for node with name: relay, " +
          "relay-mode properties: {replica-hostname=null, replica-port=9410} aren't well-formed"))));
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_missing_port() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host");

    assertThat(
      () -> DRRole.RELAY_MODE.validateRequiredProperties(relayNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("relay-mode is enabled for node with name: relay, " +
          "relay-mode properties: {replica-hostname=replica-host, replica-port=null} aren't well-formed"))));
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_valid() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    // Should not throw
    DRRole.REPLICA_MODE.validateRequiredProperties(replicaNode);
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_missing_hostname() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    assertThat(
      () -> DRRole.REPLICA_MODE.validateRequiredProperties(replicaNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("replica-mode is enabled for node with name: replica, " +
          "replica-mode properties: {relay-hostname=null, relay-port=9410, relay-group-port=9430} aren't well-formed"))));
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_missing_port() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayGroupPort(9430);

    assertThat(
      () -> DRRole.REPLICA_MODE.validateRequiredProperties(replicaNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("replica-mode is enabled for node with name: replica, " +
          "replica-mode properties: {relay-hostname=relay-host, relay-port=null, relay-group-port=9430} aren't well-formed"))));
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_missing_group_port() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410);

    assertThat(
      () -> DRRole.REPLICA_MODE.validateRequiredProperties(replicaNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("replica-mode is enabled for node with name: replica, " +
          "replica-mode properties: {relay-hostname=relay-host, relay-port=9410, relay-group-port=null} aren't well-formed"))));
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_disabled_with_partial_config() {
    Node nodeWithPartialConfig = newTestNode("node", "localhost")
      .setRelayMode(false)
      .setReplicaHostname("replica-host");

    assertThat(
      () -> DRRole.RELAY_MODE.validateRequiredProperties(nodeWithPartialConfig),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("relay-mode is disabled for node with name: node, " +
          "properties: {replica-hostname=replica-host, replica-port=null} are partially configured"))));
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_disabled_with_partial_config() {
    Node nodeWithPartialConfig = newTestNode("node", "localhost")
      .setReplicaMode(false)
      .setRelayHostname("relay-host")
      .setRelayPort(9410);

    assertThat(
      () -> DRRole.REPLICA_MODE.validateRequiredProperties(nodeWithPartialConfig),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("replica-mode is disabled for node with name: node, " +
          "properties: {relay-hostname=relay-host, relay-port=9410, relay-group-port=null} are partially configured"))));
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_disabled_with_no_properties() {
    // When relay-mode is disabled and no properties are set, it should be valid
    Node normalNode = newTestNode("node", "localhost")
      .setRelayMode(false);

    DRRole.RELAY_MODE.validateRequiredProperties(normalNode);
  }

  @Test
  public void test_validateRequiredProperties_relay_mode_disabled_with_all_properties() {
    Node nodeWithAllProps = newTestNode("node", "localhost")
      .setRelayMode(false)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    // having all properties set is fine even when mode is disabled
    DRRole.RELAY_MODE.validateRequiredProperties(nodeWithAllProps);
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_disabled_with_no_properties() {
    // When replica-mode is disabled and no properties are set, it should be valid
    Node normalNode = newTestNode("node", "localhost")
      .setReplicaMode(false);

    DRRole.REPLICA_MODE.validateRequiredProperties(normalNode);
  }

  @Test
  public void test_validateRequiredProperties_replica_mode_disabled_with_all_properties() {
    Node nodeWithAllProps = newTestNode("node", "localhost")
      .setReplicaMode(false)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    // having all properties set is fine even when mode is disabled
    DRRole.REPLICA_MODE.validateRequiredProperties(nodeWithAllProps);
  }


  @Test
  public void test_validateRequiredProperties_none_always_valid() {
    Node normalNode = newTestNode("normal", "localhost");
    DRRole.NONE.validateRequiredProperties(normalNode);
  }

  @Test
  public void test_validateAndGetRole_relay_mode_valid() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    DRRole role = DRRole.validateAndGetRole(relayNode);
    assertThat(role, is(equalTo(DRRole.RELAY_MODE)));
  }

  @Test
  public void test_validateAndGetRole_replica_mode_valid() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    DRRole role = DRRole.validateAndGetRole(replicaNode);
    assertThat(role, is(equalTo(DRRole.REPLICA_MODE)));
  }

  @Test
  public void test_validateAndGetRole_none() {
    Node normalNode = newTestNode("normal", "localhost");

    DRRole role = DRRole.validateAndGetRole(normalNode);
    assertThat(role, is(equalTo(DRRole.NONE)));
  }

  @Test
  public void test_validateAndGetRole_relay_mode_with_partial_replica_config_throws() {
    Node nodeWithPartialConfig = newTestNode("node", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410)
      .setRelayHostname("relay-host");

    assertThat(
      () -> DRRole.validateAndGetRole(nodeWithPartialConfig),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("replica-mode is disabled for node with name: node, " +
          "properties: {relay-hostname=relay-host, relay-port=null, relay-group-port=null} are partially configured"))));
  }

  @Test
  public void test_validateAndGetRole_replica_mode_with_partial_relay_config_throws() {
    Node nodeWithPartialConfig = newTestNode("node", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430)
      .setReplicaHostname("replica-host");

    assertThat(
      () -> DRRole.validateAndGetRole(nodeWithPartialConfig),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("relay-mode is disabled for node with name: node, " +
          "properties: {replica-hostname=replica-host, replica-port=null} are partially configured"))));
  }

  @Test
  public void test_validateAndGetRole_none_with_partial_relay_config_throws() {
    Node nodeWithPartialConfig = newTestNode("node", "localhost")
      .setReplicaHostname("replica-host");

    assertThat(
      () -> DRRole.validateAndGetRole(nodeWithPartialConfig),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("relay-mode is disabled for node with name: node, " +
          "properties: {replica-hostname=replica-host, replica-port=null} are partially configured"))));
  }

  @Test
  public void test_validateAndGetRole_both_modes_enabled_throws() {
    Node invalidNode = newTestNode("invalid", "localhost")
      .setRelayMode(true)
      .setReplicaMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    assertThat(
      () -> DRRole.validateAndGetRole(invalidNode),
      is(throwing(instanceOf(MalformedClusterException.class))
        .andMessage(containsString("has both relay-mode and replica-mode enabled"))));
  }

  @Test
  public void test_getRequiredProperties_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost")
      .setRelayMode(true)
      .setReplicaHostname("replica-host")
      .setReplicaPort(9410);

    Map<String, OptionalConfig<?>> requiredProperties = DRRole.RELAY_MODE.getRequiredProperties(relayNode);
    assertThat(requiredProperties.size(), is(equalTo(2)));
    assertThat(requiredProperties.containsKey(SettingName.REPLICA_HOSTNAME), is(true));
    assertThat(requiredProperties.containsKey(SettingName.REPLICA_PORT), is(true));
  }

  @Test
  public void test_getRequiredProperties_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost")
      .setReplicaMode(true)
      .setRelayHostname("relay-host")
      .setRelayPort(9410)
      .setRelayGroupPort(9430);

    Map<String, OptionalConfig<?>> requiredProperties = DRRole.REPLICA_MODE.getRequiredProperties(replicaNode);
    assertThat(requiredProperties.size(), is(equalTo(3)));
    assertThat(requiredProperties.containsKey(SettingName.RELAY_HOSTNAME), is(true));
    assertThat(requiredProperties.containsKey(SettingName.RELAY_PORT), is(true));
    assertThat(requiredProperties.containsKey(SettingName.RELAY_GROUP_PORT), is(true));
  }

  @Test
  public void test_getRequiredProperties_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.NONE.getRequiredProperties(normalNode).size(), is(equalTo(0)));
  }

  @Test
  public void test_isEnabled_relay_mode() {
    Node relayNode = newTestNode("relay", "localhost").setRelayMode(true);
    assertThat(DRRole.RELAY_MODE.isEnabled(relayNode), is(true));
    assertThat(DRRole.REPLICA_MODE.isEnabled(relayNode), is(false));
    assertThat(DRRole.NONE.isEnabled(relayNode), is(false));
  }

  @Test
  public void test_isEnabled_replica_mode() {
    Node replicaNode = newTestNode("replica", "localhost").setReplicaMode(true);
    assertThat(DRRole.RELAY_MODE.isEnabled(replicaNode), is(false));
    assertThat(DRRole.REPLICA_MODE.isEnabled(replicaNode), is(true));
    assertThat(DRRole.NONE.isEnabled(replicaNode), is(false));
  }

  @Test
  public void test_isEnabled_none() {
    Node normalNode = newTestNode("normal", "localhost");
    assertThat(DRRole.RELAY_MODE.isEnabled(normalNode), is(false));
    assertThat(DRRole.REPLICA_MODE.isEnabled(normalNode), is(false));
    assertThat(DRRole.NONE.isEnabled(normalNode), is(true));
  }
}
