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
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.terracotta.common.struct.json.StructJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.inet.json.InetJsonModule;
import org.terracotta.json.TerracottaJsonModule;

import java.net.InetSocketAddress;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * @author Mathieu Carbou
 */
public class DynamicConfigModelJsonModule extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigModelJsonModule() {
    super(DynamicConfigModelJsonModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(NodeContext.class, NodeContextMixin.class);
    setMixInAnnotation(Cluster.class, ClusterMixin.class);
    setMixInAnnotation(Stripe.class, StripeMixin.class);
    setMixInAnnotation(Node.class, NodeMixin.class);
    setMixInAnnotation(FailoverPriority.class, FailoverPriorityMixin.class);
    setMixInAnnotation(License.class, LicenseMixin.class);
  }

  @Override
  public Iterable<? extends Module> getDependencies() {
    return asList(
        new TerracottaJsonModule(),
        new StructJsonModule(),
        new InetJsonModule(),
        new Jdk8Module(),
        new JavaTimeModule());
  }

  public static class NodeContextMixin extends NodeContext {
    @JsonCreator
    public NodeContextMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                            @JsonProperty(value = "stripeId", required = true) int stripeId,
                            @JsonProperty(value = "nodeName", required = true) String nodeName) {
      super(cluster, stripeId, nodeName);
    }

    @JsonIgnore
    @Override
    public Node getNode() {
      return super.getNode();
    }

    @JsonIgnore
    @Override
    public Stripe getStripe() {
      return super.getStripe();
    }
  }

  public static class ClusterMixin extends Cluster {
    @JsonCreator
    protected ClusterMixin(@JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
      super(stripes);
    }

    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public Optional<Node> getSingleNode() throws IllegalStateException {
      return super.getSingleNode();
    }

    @JsonIgnore
    @Override
    public Optional<Stripe> getSingleStripe() {
      return super.getSingleStripe();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
      return super.isEmpty();
    }

    @JsonIgnore
    @Override
    public Collection<InetSocketAddress> getNodeAddresses() {
      return super.getNodeAddresses();
    }

    @JsonIgnore
    @Override
    public int getNodeCount() {
      return super.getNodeCount();
    }

    @JsonIgnore
    @Override
    public int getStripeCount() {
      return super.getStripeCount();
    }

    @JsonIgnore
    @Override
    public Collection<Node> getNodes() {
      return super.getNodes();
    }

    @JsonIgnore
    @Override
    public Collection<String> getDataDirNames() {
      return super.getDataDirNames();
    }
  }

  public static class StripeMixin extends Stripe {
    @JsonCreator
    public StripeMixin(@JsonProperty(value = "nodes", required = true) List<Node> nodes) {
      super(nodes);
    }

    @JsonIgnore
    @Override
    public Collection<InetSocketAddress> getNodeAddresses() {
      return super.getNodeAddresses();
    }

    @JsonIgnore
    @Override
    public Optional<Node> getSingleNode() throws IllegalStateException {
      return super.getSingleNode();
    }

    @JsonIgnore
    @Override
    public boolean isEmpty() {
      return super.isEmpty();
    }

    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public int getNodeCount() {
      return super.getNodeCount();
    }
  }

  public static class NodeMixin extends Node {
    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getAddress() {
      return super.getAddress();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getInternalAddress() {
      return super.getInternalAddress();
    }

    @JsonIgnore
    @Override
    public Optional<InetSocketAddress> getPublicAddress() {
      return super.getPublicAddress();
    }
  }

  public static class FailoverPriorityMixin extends FailoverPriority {
    public FailoverPriorityMixin(Type type, Integer voters) {
      super(type, voters);
    }

    @JsonCreator
    public static FailoverPriority valueOf(String str) {
      return FailoverPriority.valueOf(str);
    }

    @JsonValue
    @Override
    public String toString() {
      return super.toString();
    }
  }

  public static class LicenseMixin extends License {
    @JsonCreator
    public LicenseMixin(@JsonProperty(value = "capabilities", required = true) Map<String, Long> capabilityLimitMap,
                        @JsonProperty(value = "expiryDate", required = true) LocalDate expiryDate) {
      super(capabilityLimitMap, expiryDate);
    }
  }
}
