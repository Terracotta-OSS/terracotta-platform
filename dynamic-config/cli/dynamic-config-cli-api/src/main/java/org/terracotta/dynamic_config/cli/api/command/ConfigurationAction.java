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

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.inet.HostPort;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;

public abstract class ConfigurationAction extends RemoteAction {

  protected HostPort node;
  protected List<Configuration> configurations;
  protected List<ConfigurationInput> inputs;

  private final EnumSet<Setting> NOT_SUPPORTED_SETTINGS = EnumSet.of(Setting.LOCK_CONTEXT);

  protected final Operation operation;

  protected boolean isActivated;
  protected ClusterState clusterState;

  protected ConfigurationAction(Operation operation) {
    this.operation = operation;
  }

  public void setNode(HostPort node) {
    this.node = node;
  }

  public void setConfigurationInputs(List<ConfigurationInput> inputs) {
    this.inputs = inputs;
  }

  protected void validate() {

    Cluster cluster = getRuntimeCluster(node);

    // Convert the CLI inputs to Configurations.
    // To support lower-scoped settings overriding higher-scoped settings, group the
    // configurations in Scope-order (cluster-stripe-node).  Since List<> processing occurs in order
    // this will result in cluster-wide (<setting>=X) entries getting overwritten by stripe-wide
    // (stripe:<setting>=Y) entries which will be overridden by node-level (node:<setting>=Z)
    // entries for the same setting.

    Map<Scope, List<Configuration>> m = inputs.stream()
        .map(input -> input.toConfiguration(cluster))
        .flatMap(cfg -> cfg.expand().stream())
        .collect(Collectors.groupingBy(Configuration::getLevel));
    configurations = new ArrayList<>();
    configurations.addAll(m.getOrDefault(Scope.CLUSTER, new ArrayList<>()));
    configurations.addAll(m.getOrDefault(Scope.STRIPE, new ArrayList<>()));
    configurations.addAll(m.getOrDefault(Scope.NODE, new ArrayList<>()));

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
