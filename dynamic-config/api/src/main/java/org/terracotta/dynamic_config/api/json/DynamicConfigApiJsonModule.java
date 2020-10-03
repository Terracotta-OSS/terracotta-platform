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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.terracotta.common.struct.json.StructJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.DefaultApplicability;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.UnlockConfigNomadChange;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.inet.json.InetJsonModule;
import org.terracotta.json.TerracottaJsonModule;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.json.NomadJsonModule;
import org.terracotta.nomad.server.ChangeRequestState;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

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
        new NamedType(FormatUpgradeNomadChange.class, "FormatUpgradeNomadChange"),
        new NamedType(LockConfigNomadChange.class, "LockConfigNomadChange"),
        new NamedType(UnlockConfigNomadChange.class, "UnlockConfigNomadChange")
    );

    setAbstractTypes(new SimpleAbstractTypeResolver()
        .addMapping(Applicability.class, DefaultApplicability.class));

    setMixInAnnotation(DefaultApplicability.class, DefaultApplicabilityMixin.class);
    setMixInAnnotation(ClusterActivationNomadChange.class, ClusterActivationNomadChangeMixin.class);
    setMixInAnnotation(MultiSettingNomadChange.class, MultiSettingNomadChangeMixin.class);
    setMixInAnnotation(NodeAdditionNomadChange.class, NodeAdditionNomadChangeMixin.class);
    setMixInAnnotation(NodeRemovalNomadChange.class, NodeRemovalNomadChangeMixin.class);
    setMixInAnnotation(SettingNomadChange.class, SettingNomadChangeMixin.class);
    setMixInAnnotation(StripeAdditionNomadChange.class, StripeAdditionNomadChangeMixin.class);
    setMixInAnnotation(StripeRemovalNomadChange.class, StripeRemovalNomadChangeMixin.class);
    setMixInAnnotation(LockAwareDynamicConfigNomadChange.class, LockAwareDynamicConfigNomadChangeMixIn.class);
    setMixInAnnotation(FormatUpgradeNomadChange.class, FormatUpgradeNomadChangeMixIn.class);
    setMixInAnnotation(LockConfigNomadChange.class, LockConfigNomadChangeMixIn.class);
    setMixInAnnotation(UnlockConfigNomadChange.class, UnlockConfigNomadChangeMixIn.class);
    setMixInAnnotation(NomadChangeInfo.class, NomadChangeInfoMixin.class);
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

  public static class DefaultApplicabilityMixin extends DefaultApplicability {
    public DefaultApplicabilityMixin(@JsonProperty(value = "level", required = true) Scope level,
                                     @JsonProperty("stripeUID") UID stripeUID,
                                     @JsonProperty("nodeUID") UID nodeUID) {
      super(level, stripeUID, nodeUID);
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
                                        @JsonProperty(value = "stripeUID", required = true) UID stripeUID,
                                        @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, stripeUID, node);
    }
  }

  public static class NodeRemovalNomadChangeMixin extends NodeRemovalNomadChange {
    @JsonCreator
    public NodeRemovalNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                       @JsonProperty(value = "stripeUID", required = true) UID stripeUID,
                                       @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, stripeUID, node);
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

  public static class LockConfigNomadChangeMixIn extends LockConfigNomadChange {
    @JsonCreator
    public LockConfigNomadChangeMixIn(@JsonProperty(value = "lockContext", required = true) LockContext lockContext) {
      super(lockContext);
    }
  }

  public static class UnlockConfigNomadChangeMixIn extends UnlockConfigNomadChange {
    @JsonCreator
    public UnlockConfigNomadChangeMixIn(@JsonProperty(value = "forced", required = true) boolean forced) {
      super(forced);
    }
  }

  public static class NomadChangeInfoMixin extends NomadChangeInfo {
    @JsonCreator
    public NomadChangeInfoMixin(@JsonProperty(value = "changeUuid", required = true) UUID changeUuid,
                                @JsonProperty(value = "nomadChange", required = true) NomadChange nomadChange,
                                @JsonProperty(value = "changeRequestState", required = true) ChangeRequestState changeRequestState,
                                @JsonProperty(value = "version", required = true) long version,
                                @JsonProperty(value = "creationHost", required = true) String creationHost,
                                @JsonProperty(value = "creationUser", required = true) String creationUser,
                                @JsonProperty(value = "creationTimestamp", required = true) Instant creationTimestamp,
                                @JsonProperty(value = "changeResultHash", required = true) String changeResultHash) {
      super(changeUuid, nomadChange, changeRequestState, version, creationHost, creationUser, creationTimestamp, changeResultHash);
    }
  }
}
