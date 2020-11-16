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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Setting;

import java.net.InetSocketAddress;
import java.util.EnumSet;
import java.util.List;

import static java.lang.String.format;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;

public abstract class ConfigurationCommand extends RemoteCommand {

  protected InetSocketAddress node;
  protected List<Configuration> configurations;

  private final EnumSet<Setting> NOT_SUPPORTED_SETTINGS = EnumSet.of(Setting.LOCK_CONTEXT);

  protected final Operation operation;

  protected boolean isActivated;
  protected ClusterState clusterState;

  protected ConfigurationCommand(Operation operation) {
    this.operation = operation;
  }

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setConfigurations(List<Configuration> configurations) {
    this.configurations = configurations;
  }

  public void validate() {
    isActivated = isActivated(node);
    clusterState = isActivated ? ACTIVATED : CONFIGURING;

    // validate all configurations passes on CLI
    for (Configuration configuration : configurations) {
      if (NOT_SUPPORTED_SETTINGS.contains(configuration.getSetting())) {
        throw new IllegalArgumentException(format("'%s' is not supported", configuration.getSetting()));
      }
      configuration.validate(clusterState, operation);
    }

    // once valid, check for duplicates
    for (int i = 0; i < configurations.size(); i++) {
      Configuration first = configurations.get(i);
      for (int j = i + 1; j < configurations.size(); j++) {
        Configuration second = configurations.get(j);
        if (second.duplicates(first)) {
          throw new IllegalArgumentException("Duplicate configurations found: " + first + " and " + second);
        }
      }
    }
  }
}
