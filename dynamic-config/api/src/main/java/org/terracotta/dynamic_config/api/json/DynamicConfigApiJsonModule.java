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
package org.terracotta.dynamic_config.api.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.terracotta.common.struct.json.StructJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.inet.json.InetJsonModule;
import org.terracotta.json.TerracottaJsonModule;
import org.terracotta.nomad.json.NomadJsonModule;

import java.net.InetSocketAddress;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigApiJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigApiJsonModule() {
    super(DynamicConfigApiJsonModule.class.getSimpleName(), new com.fasterxml.jackson.core.Version(1, 0, 0, null, null, null));

    registerSubtypes(
        new NamedType(ClusterActivationNomadChange.class, "ClusterActivationNomadChange"),
        new NamedType(MultiSettingNomadChange.class, "MultiSettingNomadChange"),
        new NamedType(NodeAdditionNomadChange.class, "NodeAdditionNomadChange"),
        new NamedType(ClusterActivationNomadChange.class, "ClusterActivationNomadChange"),
        new NamedType(NodeRemovalNomadChange.class, "NodeRemovalNomadChange"),
        new NamedType(SettingNomadChange.class, "SettingNomadChange"),
        new NamedType(StripeAdditionNomadChange.class, "StripeAdditionNomadChange"),
        new NamedType(StripeRemovalNomadChange.class, "StripeRemovalNomadChange"),
        new NamedType(LockAwareDynamicConfigNomadChange.class, "LockAwareDynamicConfigNomadChange"),
        new NamedType(FormatUpgradeNomadChange.class, "FormatUpgradeNomadChange")
    );

    setMixInAnnotation(NodeNomadChange.class, NodeNomadChangeMixin.class);
    setMixInAnnotation(Applicability.class, ApplicabilityMixin.class);
    setMixInAnnotation(ClusterActivationNomadChange.class, ClusterActivationNomadChangeMixin.class);
    setMixInAnnotation(MultiSettingNomadChange.class, MultiSettingNomadChangeMixin.class);
    setMixInAnnotation(NodeAdditionNomadChange.class, NodeAdditionNomadChangeMixin.class);
    setMixInAnnotation(NodeRemovalNomadChange.class, NodeRemovalNomadChangeMixin.class);
    setMixInAnnotation(SettingNomadChange.class, SettingNomadChangeMixin.class);
    setMixInAnnotation(StripeAdditionNomadChange.class, StripeAdditionNomadChangeMixin.class);
    setMixInAnnotation(StripeRemovalNomadChange.class, StripeRemovalNomadChangeMixin.class);
    setMixInAnnotation(LockAwareDynamicConfigNomadChange.class, LockAwareDynamicConfigNomadChangeMixIn.class);
    setMixInAnnotation(FormatUpgradeNomadChange.class, FormatUpgradeNomadChangeMixIn.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return asList(
        new TerracottaJsonModule(),
        new StructJsonModule(),
        new InetJsonModule(),
        new NomadJsonModule(),
        new Jdk8Module(),
        new JavaTimeModule(),
        new DynamicConfigModelJsonModule());
  }

  public static abstract class NodeNomadChangeMixin extends NodeNomadChange {
    public NodeNomadChangeMixin(Cluster updated, int stripeId, Node node) {
      super(updated, stripeId, node);
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getNodeAddress() {
      return super.getNodeAddress();
    }
  }

  public static class ApplicabilityMixin extends Applicability {
    @JsonCreator
    protected ApplicabilityMixin(@JsonProperty(value = "level", required = true) Scope level,
                                 @JsonProperty("stripeId") Integer stripeId,
                                 @JsonProperty("nodeName") String nodeName) {
      super(level, stripeId, nodeName);
    }
  }

  public static class ClusterActivationNomadChangeMixin extends ClusterActivationNomadChange {
    @JsonCreator
    public ClusterActivationNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster) {
      super(cluster);
    }
  }

  public static class MultiSettingNomadChangeMixin extends MultiSettingNomadChange {
    @JsonCreator
    public MultiSettingNomadChangeMixin(@JsonProperty(value = "changes", required = true) List<SettingNomadChange> changes) {
      super(changes);
    }
  }

  public static class NodeAdditionNomadChangeMixin extends NodeAdditionNomadChange {
    @JsonCreator
    public NodeAdditionNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                        @JsonProperty(value = "stripeId", required = true) int stripeId,
                                        @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, stripeId, node);
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getNodeAddress() {
      return super.getNodeAddress();
    }
  }

  public static class NodeRemovalNomadChangeMixin extends NodeRemovalNomadChange {
    @JsonCreator
    public NodeRemovalNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                       @JsonProperty(value = "stripeId", required = true) int stripeId,
                                       @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, stripeId, node);
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getNodeAddress() {
      return super.getNodeAddress();
    }
  }

  public static class SettingNomadChangeMixin extends SettingNomadChange {
    @JsonCreator
    protected SettingNomadChangeMixin(@JsonProperty(value = "applicability", required = true) Applicability applicability,
                                      @JsonProperty(value = "operation", required = true) Operation operation,
                                      @JsonProperty(value = "setting", required = true) Setting setting,
                                      @JsonProperty(value = "name") String name,
                                      @JsonProperty(value = "value") String value) {
      super(applicability, operation, setting, name, value);
    }
  }

  public static class StripeAdditionNomadChangeMixin extends StripeAdditionNomadChange {
    @JsonCreator
    public StripeAdditionNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                          @JsonProperty(value = "stripe", required = true) Stripe stripe) {
      super(cluster, stripe);
    }
  }

  public static class StripeRemovalNomadChangeMixin extends StripeRemovalNomadChange {
    @JsonCreator
    public StripeRemovalNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                         @JsonProperty(value = "stripe", required = true) Stripe stripe) {
      super(cluster, stripe);
    }
  }

  public static class LockAwareDynamicConfigNomadChangeMixIn extends LockAwareDynamicConfigNomadChange {
    @JsonCreator
    public LockAwareDynamicConfigNomadChangeMixIn(@JsonProperty(value = "lockToken", required = true) String lockToken,
                                                  @JsonProperty(value = "change", required = true) DynamicConfigNomadChange change) {
      super(lockToken, change);
    }
  }

  public static class FormatUpgradeNomadChangeMixIn extends FormatUpgradeNomadChange {
    @JsonCreator
    public FormatUpgradeNomadChangeMixIn(@JsonProperty(value = "from", required = true) Version from,
                                         @JsonProperty(value = "to", required = true) Version to) {
      super(from, to);
    }
  }
}
