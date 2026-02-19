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
  RELAY(SettingName.RELAY_MODE) {
    @Override
    public boolean isEnabled(Node node) {
      return node.getRelayMode().orDefault();
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

  REPLICA(SettingName.REPLICA_MODE) {
    @Override
    public boolean isEnabled(Node node) {
      return node.getReplicaMode().orDefault();
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
      throw new AssertionError("Node with name: " + node.getName() + " has both relay-mode and replica-mode enabled. " +
        "A node cannot have both relay-mode and replica-mode active");
    }

    if (relayMode) return RELAY;
    if (replicaMode) return REPLICA;
    return NONE;
  }
}
