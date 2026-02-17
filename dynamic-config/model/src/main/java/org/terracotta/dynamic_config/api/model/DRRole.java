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
}
