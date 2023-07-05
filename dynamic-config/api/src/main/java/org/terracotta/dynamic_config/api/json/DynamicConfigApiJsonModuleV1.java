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
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.FormatUpgrade;
import org.terracotta.json.Json;

import java.util.List;

import static java.util.Collections.singletonList;

/**
 * This module can be added to the existing ones and will override some definitions to make the object mapper compatible with V1
 *
 * @author Mathieu Carbou
 * @deprecated old V1 format. Do not use anymore. Here for reference and backward compatibility.
 */
@Deprecated
public class DynamicConfigApiJsonModuleV1 extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public DynamicConfigApiJsonModuleV1() {
    super(DynamicConfigApiJsonModuleV1.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    addAbstractTypeMapping(Applicability.class, Applicability.V1.class);

    registerSubtypes(
        new NamedType(NodeAdditionNomadChangeV1.class, "NodeAdditionNomadChange"),
        new NamedType(NodeRemovalNomadChangeV1.class, "NodeRemovalNomadChange")
    );

    setMixInAnnotation(Cluster.class, ClusterMixin.class);
    setMixInAnnotation(Applicability.V1.class, ApplicabilityV1Mixin.class);
    setMixInAnnotation(Applicability.V2.class, ApplicabilityV2Mixin.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return singletonList(new DynamicConfigModelJsonModuleV1());
  }

  @JsonPropertyOrder(value = {"name", "stripe"}, alphabetic = true)
  public static class ClusterMixin extends DynamicConfigModelJsonModule.ClusterMixin {
    @JsonCreator
    protected ClusterMixin(@JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
      super(stripes);
    }
  }

  public static class ApplicabilityV2Mixin extends Applicability.V2 {
    public ApplicabilityV2Mixin(@JsonProperty(value = "level", required = true) Scope level,
                                @JsonProperty("stripeUID") UID stripeUID,
                                @JsonProperty("nodeUID") UID nodeUID) {
      super(level, stripeUID, nodeUID);
    }

    @JsonProperty("scope")
    @Override
    public Scope getLevel() {
      return super.getLevel();
    }
  }

  public static abstract class ApplicabilityV1Mixin extends Applicability.V1 {
    public ApplicabilityV1Mixin(@JsonProperty(value = "scope", required = true) Scope scope,
                                @JsonProperty("stripeId") Integer stripeId,
                                @JsonProperty("nodeName") String nodeName) {
      super(scope, stripeId, nodeName);
    }

    @JsonProperty("scope")
    @Override
    public Scope getLevel() {
      return super.getLevel();
    }
  }

  public static class NodeAdditionNomadChangeV1 extends NodeAdditionNomadChange {
    private final Cluster clusterV1;
    private final int stripeId;
    private final Node nodeV1;

    @JsonCreator
    public NodeAdditionNomadChangeV1(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                     @JsonProperty(value = "stripeId", required = true) int stripeId,
                                     @JsonProperty(value = "node", required = true) Node node) {
      super(
          upgrade(cluster),
          upgrade(cluster).getStripe(stripeId).get().getUID(),
          upgrade(cluster).getNodeByName(node.getName()).get());

      this.clusterV1 = cluster;
      this.stripeId = stripeId;
      this.nodeV1 = node;
    }

    public int getStripeId() {
      return stripeId;
    }

    @JsonIgnore
    @Override
    public Cluster getCluster() {
      return super.getCluster();
    }

    @JsonIgnore
    @Override
    public UID getStripeUID() {
      return super.getStripeUID();
    }

    @JsonProperty("cluster")
    public Cluster getClusterV1() {
      return clusterV1;
    }

    @JsonProperty("node")
    public Node getNodeV1() {
      return nodeV1;
    }

    private static Cluster upgrade(Cluster cluster) {
      return new FormatUpgrade().upgrade(cluster, org.terracotta.dynamic_config.api.model.Version.V1);
    }
  }

  public static class NodeRemovalNomadChangeV1 extends NodeRemovalNomadChange {
    private final Cluster clusterV1;
    private final int stripeId;
    private final Node nodeV1;

    @JsonCreator
    public NodeRemovalNomadChangeV1(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                    @JsonProperty(value = "stripeId", required = true) int stripeId,
                                    @JsonProperty(value = "node", required = true) Node node) {
      super(
          upgrade(cluster),
          upgrade(cluster).getStripe(stripeId).get().getUID(),
          node);

      this.clusterV1 = cluster;
      this.stripeId = stripeId;
      this.nodeV1 = node;
    }

    public int getStripeId() {
      return stripeId;
    }

    @JsonIgnore
    @Override
    public Cluster getCluster() {
      return super.getCluster();
    }

    @JsonIgnore
    @Override
    public UID getStripeUID() {
      return super.getStripeUID();
    }

    @JsonProperty("cluster")
    public Cluster getClusterV1() {
      return clusterV1;
    }

    @JsonProperty("node")
    public Node getNodeV1() {
      return nodeV1;
    }

    private static Cluster upgrade(Cluster cluster) {
      return new FormatUpgrade().upgrade(cluster, org.terracotta.dynamic_config.api.model.Version.V1);
    }
  }
}
