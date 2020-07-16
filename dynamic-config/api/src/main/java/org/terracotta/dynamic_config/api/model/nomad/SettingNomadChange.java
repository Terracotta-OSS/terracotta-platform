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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Setting;

import java.util.Objects;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.ClusterState.ACTIVATED;

/**
 * Nomad change that supports any dynamic config change (see Cluster-tool.adoc)
 *
 * @author Mathieu Carbou
 */
public class SettingNomadChange extends FilteredNomadChange {

  private final Operation operation;
  private final Setting setting;
  private final String name;
  private final String value;

  protected SettingNomadChange(Applicability applicability,
                               Operation operation,
                               Setting setting,
                               String name,
                               String value) {
    super(applicability);
    this.operation = requireNonNull(operation);
    this.setting = requireNonNull(setting);
    this.name = name;
    this.value = value;
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  @Override
  public String getSummary() {
    String s = operation == Operation.SET ?
        name == null ? (operation + " " + setting + "=" + value) : (operation + " " + setting + "." + name + "=" + value) :
        name == null ? (operation + " " + setting) : (operation + " " + setting + "." + name);
    switch (getApplicability().getScope()) {
      case STRIPE:
        return s + " (stripe ID: " + getApplicability().getStripeId().getAsInt() + ")";
      case NODE:
        return s + " (stripe ID: " + getApplicability().getStripeId().getAsInt() + ", node: " + getApplicability().getNodeName() + ")";
      default:
        return s;
    }
  }

  @Override
  public Cluster apply(Cluster original) {
    Cluster updated = original.clone();
    Configuration configuration = toConfiguration(updated);
    configuration.validate(ACTIVATED, getOperation());
    configuration.apply(updated);
    return updated;
  }

  @Override
  public boolean canApplyAtRuntime() {
    return !getSetting().isRestartRequired();
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public Setting getSetting() {
    return setting;
  }

  public Operation getOperation() {
    return operation;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof SettingNomadChange)) return false;
    if (!super.equals(o)) return false;
    SettingNomadChange that = (SettingNomadChange) o;
    return getOperation() == that.getOperation() &&
        getSetting() == that.getSetting() &&
        Objects.equals(getName(), that.getName()) &&
        Objects.equals(getValue(), that.getValue());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getOperation(), getSetting(), getName(), getValue());
  }

  @Override
  public String toString() {
    return "SettingNomadChange{" +
        "operation=" + operation +
        ", setting=" + setting +
        ", name='" + name + '\'' +
        ", value='" + value + '\'' +
        ", applicability=" + getApplicability() +
        '}';
  }

  public Configuration toConfiguration(Cluster cluster) {
    switch (operation) {
      case SET:
        return name == null ?
            Configuration.valueOf(namespace(cluster) + setting + "=" + value) :
            Configuration.valueOf(namespace(cluster) + setting + "." + name + "=" + value);
      case UNSET:
        return name == null ?
            Configuration.valueOf(namespace(cluster) + setting.toString()) :
            Configuration.valueOf(namespace(cluster) + setting + "." + name);
      default:
        throw new AssertionError(operation);
    }
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private String namespace(Cluster cluster) {
    switch (getApplicability().getScope()) {
      case CLUSTER:
        return "";
      case STRIPE: {
        int stripeId = getApplicability().getStripeId().getAsInt();
        if (stripeId < 1) {
          throw new IllegalArgumentException("Invalid stripe ID: " + stripeId);
        }
        if (stripeId > cluster.getStripeCount()) {
          throw new IllegalArgumentException("Stripe ID: " + stripeId + " not found in cluster: " + cluster.toShapeString());
        }
        return "stripe." + stripeId + ".";
      }
      case NODE: {
        int stripeId = getApplicability().getStripeId().getAsInt();
        String nodeName = getApplicability().getNodeName();
        int nodeId = cluster.getNodeId(stripeId, nodeName)
            .orElseThrow(() -> new IllegalArgumentException("Node: " + nodeName + " in stripe ID: " + stripeId + " not found in cluster: " + cluster.toShapeString()));
        return "stripe." + stripeId + ".node." + nodeId + ".";
      }
      default:
        throw new AssertionError(getApplicability().getScope());
    }
  }

  public static SettingNomadChange set(Applicability applicability, Setting type, String name, String value) {
    return new SettingNomadChange(applicability, Operation.SET, type, name, value);
  }

  public static SettingNomadChange unset(Applicability applicability, Setting type, String name) {
    return new SettingNomadChange(applicability, Operation.UNSET, type, name, null);
  }

  public static SettingNomadChange set(Applicability applicability, Setting type, String value) {
    return new SettingNomadChange(applicability, Operation.SET, type, null, value);
  }

  public static SettingNomadChange unset(Applicability applicability, Setting type) {
    return new SettingNomadChange(applicability, Operation.UNSET, type, null, null);
  }

  public static SettingNomadChange fromConfiguration(Configuration configuration, Operation operation, Cluster cluster) {
    Applicability applicability = toApplicability(configuration, cluster);
    switch (operation) {
      case SET:
        return SettingNomadChange.set(applicability, configuration.getSetting(), configuration.getKey(), configuration.getValue());
      case UNSET:
        return SettingNomadChange.unset(applicability, configuration.getSetting(), configuration.getKey());
      default:
        throw new IllegalArgumentException("Operation " + operation + " cannot be converted to a Nomad change for an active cluster");
    }
  }

  @SuppressWarnings("OptionalGetWithoutIsPresent")
  private static Applicability toApplicability(Configuration configuration, Cluster cluster) {
    switch (configuration.getScope()) {
      case NODE:
        return Applicability.node(configuration.getStripeId(), cluster.getNode(configuration.getStripeId(), configuration.getNodeId()).get().getNodeName());
      case STRIPE:
        return Applicability.stripe(configuration.getStripeId());
      case CLUSTER:
        return Applicability.cluster();
      default:
        throw new AssertionError(configuration.getScope());
    }
  }
}
