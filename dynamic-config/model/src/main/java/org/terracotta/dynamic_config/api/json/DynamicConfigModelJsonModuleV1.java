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
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * This module can be added to the existing ones and will override some definitions to make the object mapper compatible with V1
 *
 * @author Mathieu Carbou
 */
public class DynamicConfigModelJsonModuleV1 extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigModelJsonModuleV1() {
    super(DynamicConfigModelJsonModuleV1.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(Cluster.class, ClusterMixinV1.class);
    setMixInAnnotation(Stripe.class, StripeMixinV1.class);
    setMixInAnnotation(Node.class, NodeMixinV1.class);
    setMixInAnnotation(FailoverPriority.class, FailoverPriorityMixin.class);
    setMixInAnnotation(License.class, LicenseMixinV1.class);
  }

  public static class ClusterMixinV1 extends Cluster {
    @JsonCreator
    protected ClusterMixinV1(@JsonProperty(value = "stripes", required = true) List<Stripe> stripes) {
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

  public static class StripeMixinV1 extends Stripe {
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

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  public static class NodeMixinV1 extends Node {

    @JsonProperty("nodeName") String name;
    @JsonProperty("nodeHostname") String hostname;
    @JsonProperty("nodePublicHostname") String publicHostname;
    @JsonProperty("nodePort") Integer port;
    @JsonProperty("nodePublicPort") Integer publicPort;
    @JsonProperty("nodeGroupPort") Integer groupPort;
    @JsonProperty("nodeBindAddress") String bindAddress;
    @JsonProperty("nodeGroupBindAddress") String groupBindAddress;
    @JsonProperty("nodeMetadataDir") Path metadataDir;
    @JsonProperty("nodeLogDir") Path logDir;
    @JsonProperty("nodeBackupDir") Path backupDir;
    @JsonProperty("nodeLoggerOverrides") Map<String, String> loggerOverrides;

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

  public static class LicenseMixinV1 extends License {
    @JsonCreator
    public LicenseMixinV1(@JsonProperty(value = "capabilities", required = true) Map<String, Long> capabilityLimitMap,
                          @JsonProperty(value = "expiryDate", required = true) LocalDate expiryDate) {
      super(capabilityLimitMap, Collections.emptyMap(), expiryDate);
    }
  }
}
