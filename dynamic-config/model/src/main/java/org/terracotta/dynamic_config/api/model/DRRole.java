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

import org.terracotta.dynamic_config.api.service.MalformedClusterException;
import org.terracotta.inet.HostPort;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_GROUP_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.RELAY_PORT;
import static org.terracotta.dynamic_config.api.model.SettingName.REPLICA_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.SettingName.REPLICA_PORT;

public enum DRRole {
  RELAY_MODE(SettingName.RELAY_MODE) {
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

    /**
     * if relay-mode is enabled, peer should be present
     */
    @Override
    public InetSocketAddress getPeer(Node node) {
      if (isEnabled(node)) {
        return node.getReplicaHostPort()
          .map(HostPort::createInetSocketAddress)
          .orElseThrow(AssertionError::new);
      }
      return null;
    }
  },

  REPLICA_MODE(SettingName.REPLICA_MODE) {
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

    /**
     * if replica-mode is enabled, peer should be present
     */
    @Override
    public InetSocketAddress getPeer(Node node) {
      if (isEnabled(node)) {
        return node.getRelayHostPort()
          .map(HostPort::createInetSocketAddress)
          .orElseThrow(AssertionError::new);
      }
      return null;
    }

    /**
     * if replica-mode is enabled, peer group-port should be present
     */
    @Override
    public InetSocketAddress getPeerGroupPort(Node node) {
      if (isEnabled(node)) {
        return node.getRelayHostGroupPort()
          .map(HostPort::createInetSocketAddress)
          .orElseThrow(AssertionError::new);
      }
      return null;
    }
  },

  NONE("none") {
    @Override
    public boolean isEnabled(Node node) {
      return !RELAY_MODE.isEnabled(node) && !REPLICA_MODE.isEnabled(node);
    }

    @Override
    public Map<String, OptionalConfig<?>> getRequiredProperties(Node node) {
      return Collections.emptyMap();
    }
  };

  private final String label;

  DRRole(String label) {
    this.label = label;
  }

  public String getLabel() {
    return label;
  }

  public abstract boolean isEnabled(Node node);

  public abstract Map<String, OptionalConfig<?>> getRequiredProperties(Node node);

  public InetSocketAddress getPeer(Node node) {
    return null;
  }

  public InetSocketAddress getPeerGroupPort(Node node) {
    return null;
  }

  public static DRRole validateAndGetRole(Node node) {
    // when mode is disabled - check properties are either fully set or not set at all in a node (catch partial configs)
    for (DRRole role : values()) {
      role.validateRequiredProperties(node);
    }

    return fromNode(node);
  }

  public void validateRequiredProperties(Node node) {
    if (this == NONE) {
      return;
    }

    Map<String, OptionalConfig<?>> requiredProps = getRequiredProperties(node);
    long configuredCount = requiredProps.values().stream()
      .filter(OptionalConfig::isConfigured)
      .count();

    if (isEnabled(node)) {
      if (configuredCount != requiredProps.size()) {
        Map<String, Object> inconsistent = new LinkedHashMap<>();
        requiredProps.forEach((key, value) -> inconsistent.put(key, String.valueOf(value.orDefault())));
        throw new MalformedClusterException(getLabel() + " is enabled for node with name: " + node.getName() +
          ", " + getLabel() + " properties: " + inconsistent + " aren't well-formed");
      }
    } else {
      // when mode is disabled and the user sets partial configuration for a node
      if (configuredCount > 0 && configuredCount < requiredProps.size()) {
        Map<String, Object> inconsistent = new LinkedHashMap<>();
        requiredProps.forEach((key, value) -> inconsistent.put(key, String.valueOf(value.orDefault())));
        throw new MalformedClusterException(getLabel() + " is disabled for node with name: " + node.getName() +
          ", properties: " + inconsistent + " are partially configured. Either remove all properties or set all required properties.");
      }
    }
  }

  public static DRRole fromNode(Node node) {
    boolean relayMode = RELAY_MODE.isEnabled(node);
    boolean replicaMode = REPLICA_MODE.isEnabled(node);

    if (relayMode && replicaMode) {
      throw new MalformedClusterException("Node with name: " + node.getName() + " has both relay-mode and replica-mode enabled. " +
        "A node cannot have both relay-mode and replica-mode active");
    }

    if (relayMode) return RELAY_MODE;
    if (replicaMode) return REPLICA_MODE;
    return NONE;
  }
}
