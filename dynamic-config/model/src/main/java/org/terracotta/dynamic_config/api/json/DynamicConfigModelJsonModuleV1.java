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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

/**
 * This module can be added to the existing ones and will override some definitions to make the object mapper compatible with V1
 *
 * @author Mathieu Carbou
 * @deprecated old V1 format. Do not use anymore. Here for reference and backward compatibility.
 */
@Deprecated
public class DynamicConfigModelJsonModuleV1 extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigModelJsonModuleV1() {
    super(DynamicConfigModelJsonModuleV1.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(Node.class, NodeMixinV1.class);
    setMixInAnnotation(License.class, LicenseMixin.class);
    setMixInAnnotation(NodeContext.class, NodeContextMixin.class);
  }

  public static class NodeContextMixin extends NodeContext {
    @JsonCreator
    public NodeContextMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                            @JsonProperty(value = "stripeId", required = false) int stripeId,
                            @JsonProperty(value = "nodeName", required = true) String nodeName) {
      super(cluster, cluster.getNodeByName(nodeName).get().getUID());
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

    @JsonIgnore
    @Override
    public UID getStripeUID() {
      return super.getStripeUID();
    }
  }

  @SuppressFBWarnings("EQ_DOESNT_OVERRIDE_EQUALS")
  public static class NodeMixinV1 extends Node {

    @JsonProperty("nodeName")
    String name;
    @JsonProperty("nodeHostname")
    String hostname;
    @JsonProperty("nodePublicHostname")
    String publicHostname;
    @JsonProperty("nodePort")
    Integer port;
    @JsonProperty("nodePublicPort")
    Integer publicPort;
    @JsonProperty("nodeGroupPort")
    Integer groupPort;
    @JsonProperty("nodeBindAddress")
    String bindAddress;
    @JsonProperty("nodeGroupBindAddress")
    String groupBindAddress;
    @JsonProperty("nodeMetadataDir")
    Path metadataDir;
    @JsonProperty("nodeLogDir")
    Path logDir;
    @JsonProperty("nodeBackupDir")
    Path backupDir;
    @JsonProperty("nodeLoggerOverrides")
    Map<String, String> loggerOverrides;

    @JsonIgnore
    @Override
    public Scope getScope() {
      return super.getScope();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getBindSocketAddress() {
      return super.getBindSocketAddress();
    }

    @JsonIgnore
    @Override
    public InetSocketAddress getInternalSocketAddress() {
      return super.getInternalSocketAddress();
    }

    @JsonIgnore
    @Override
    public Optional<InetSocketAddress> getPublicSocketAddress() {
      return super.getPublicSocketAddress();
    }

    @JsonIgnore
    @Override
    public Endpoint getBindEndpoint() {
      return super.getBindEndpoint();
    }

    @JsonIgnore
    @Override
    public Endpoint getInternalEndpoint() {
      return super.getInternalEndpoint();
    }

    @JsonIgnore
    @Override
    public Optional<Endpoint> getPublicEndpoint() {
      return super.getPublicEndpoint();
    }
  }

  public static class LicenseMixin extends License {
    @JsonCreator
    public LicenseMixin(@JsonProperty(value = "capabilities", required = true) Map<String, Long> capabilityLimitMap,
                        @JsonProperty(value = "expiryDate", required = true) LocalDate expiryDate) {
      super(capabilityLimitMap, Collections.emptyMap(), expiryDate);
    }
  }
}
