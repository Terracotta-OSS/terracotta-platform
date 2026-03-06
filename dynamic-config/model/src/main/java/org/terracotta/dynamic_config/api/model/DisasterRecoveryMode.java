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

import org.terracotta.inet.HostPort;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.REPLICA_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.REPLICA_PORT;

public enum DisasterRecoveryMode {
  /**
   * When in RELAY mode, a node acts as a data relay in disaster recovery scenarios.
   * <p>
   * A node in RELAY mode operates in the {@link org.terracotta.diagnostic.model.LogicalServerState#PASSIVE_RELAY PASSIVE_RELAY} state
   * and is responsible for sending data to its peer REPLICA node. There is a one-to-one mapping between
   * a RELAY node and a REPLICA node.
   * <p>
   * Required properties when in RELAY mode:
   * <ul>
   *   <li>{@code replica-hostname}: The hostname of the replica node to send data to</li>
   *   <li>{@code replica-port}: The port of the replica node to send data to</li>
   * </ul>
   * <p>
   * A node cannot be both a RELAY and a REPLICA simultaneously.
   */
  RELAY(SettingName.RELAY) {
    @Override
    public boolean isEnabled(Node node) {
      return node.getRelay().orDefault();
    }

    @Override
    public Map<String, OptionalConfig<?>> getRequiredProperties(Node node) {
      Map<String, OptionalConfig<?>> props = new LinkedHashMap<>();
      props.put(REPLICA_HOSTNAME, node.getReplicaHostname());
      props.put(REPLICA_PORT, node.getReplicaPort());
      return props;
    }

    @Override
    public Optional<InetSocketAddress> getPeer(Node node) {
      return node.getReplicaHostPort().map(HostPort::createInetSocketAddress);
    }
  },

  /**
   * When in REPLICA mode, a node acts as a data replica in disaster recovery scenarios.
   * <p>
   * A node in REPLICA mode operates in one of two states:
   * <ul>
   *   <li>{@link org.terracotta.diagnostic.model.LogicalServerState#PASSIVE_REPLICA_START PASSIVE_REPLICA_START}:
   *       Initial state where the replica is requesting to receive data from its relay node</li>
   *   <li>{@link org.terracotta.diagnostic.model.LogicalServerState#PASSIVE_REPLICA PASSIVE_REPLICA}:
   *       Final state where the replica is actively receiving and storing replicated data from its relay node</li>
   * </ul>
   * <p>
   * Required properties when in REPLICA mode:
   * <ul>
   *   <li>{@code relay-hostname}: The hostname of the relay node to receive data from</li>
   *   <li>{@code relay-port}: The port of the relay node to receive data from</li>
   *   <li>{@code relay-group-port}: The group port of the relay node to receive data from</li>
   * </ul>
   * <p>
   * A node cannot be both a RELAY and a REPLICA simultaneously.
   */
  REPLICA(SettingName.REPLICA) {
    @Override
    public boolean isEnabled(Node node) {
      return node.getReplica().orDefault();
    }

    @Override
    public Map<String, OptionalConfig<?>> getRequiredProperties(Node node) {
      Map<String, OptionalConfig<?>> props = new LinkedHashMap<>();
      props.put(RELAY_HOSTNAME, node.getRelayHostname());
      props.put(RELAY_PORT, node.getRelayPort());
      props.put(RELAY_GROUP_PORT, node.getRelayGroupPort());
      return props;
    }

    @Override
    public Optional<InetSocketAddress> getPeer(Node node) {
      return node.getRelayHostPort().map(HostPort::createInetSocketAddress);
    }

    @Override
    public Optional<InetSocketAddress> getPeerGroupPort(Node node) {
      return node.getRelayHostGroupPort().map(HostPort::createInetSocketAddress);
    }
  },

  /**
   * When in NONE mode, the node is not configured for disaster recovery.
   * <p>
   * A node in NONE mode operates as a standard node (active or passive) without any relay or replica functionality.
   * This is the default mode when neither relay nor replica is enabled.
   */
  NONE("none") {
    @Override
    public boolean isEnabled(Node node) {
      return !RELAY.isEnabled(node) && !REPLICA.isEnabled(node);
    }

    @Override
    public Map<String, OptionalConfig<?>> getRequiredProperties(Node node) {
      return Collections.emptyMap();
    }
  };

  private final String label;

  DisasterRecoveryMode(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public abstract boolean isEnabled(Node node);

  public abstract Map<String, OptionalConfig<?>> getRequiredProperties(Node node);

  public Optional<InetSocketAddress> getPeer(Node node) {
    return Optional.empty();
  }

  public Optional<InetSocketAddress> getPeerGroupPort(Node node) {
    return Optional.empty();
  }

  public static DisasterRecoveryMode fromNode(Node node) {
    boolean relayMode = RELAY.isEnabled(node);
    boolean replicaMode = REPLICA.isEnabled(node);

    if (relayMode && replicaMode) {
      throw new AssertionError("Node with name: " + node.getName() + " has both relay and replica settings enabled");
    }

    if (relayMode) return RELAY;
    if (replicaMode) return REPLICA;
    return NONE;
  }

  public static boolean isReplica(Node node) {
    return fromNode(node) == REPLICA;
  }

  public static boolean isRelay(Node node) {
    return fromNode(node) == RELAY;
  }
}
