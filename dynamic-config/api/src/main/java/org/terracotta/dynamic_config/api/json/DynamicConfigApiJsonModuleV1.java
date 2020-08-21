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
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.DefaultApplicability;
import org.terracotta.dynamic_config.api.model.nomad.NodeAdditionNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;

/**
 * This module can be added to the existing ones and will override some definitions to make the object mapper compatible with V1
 *
 * @author Mathieu Carbou
 * @deprecated old V1 format. Do not use anymore. Here for reference and backward compatibility.
 */
@Deprecated
public class DynamicConfigApiJsonModuleV1 extends SimpleModule {
  private static final long serialVersionUID = 1L;

  public DynamicConfigApiJsonModuleV1() {
    super(DynamicConfigApiJsonModuleV1.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setAbstractTypes(new SimpleAbstractTypeResolver()
        .addMapping(Applicability.class, ApplicabilityV1.class));

    setMixInAnnotation(ApplicabilityV1.class, ApplicabilityV1Mixin.class);
    setMixInAnnotation(DefaultApplicability.class, DefaultApplicabilityMixin.class);
  }

  public static class DefaultApplicabilityMixin extends DefaultApplicability {
    public DefaultApplicabilityMixin(@JsonProperty(value = "level", required = true) Scope level,
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

  public static abstract class ApplicabilityV1Mixin extends ApplicabilityV1 {
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

  public static class NodeAdditionNomadChangeMixin extends NodeAdditionNomadChange {
    @JsonCreator
    public NodeAdditionNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                        @JsonProperty(value = "stripeId", required = true) int stripeId,
                                        @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, cluster.getStripe(stripeId).get().getUID(), node);
    }
  }

  public static class NodeRemovalNomadChangeMixin extends NodeRemovalNomadChange {
    @JsonCreator
    public NodeRemovalNomadChangeMixin(@JsonProperty(value = "cluster", required = true) Cluster cluster,
                                       @JsonProperty(value = "stripeId", required = true) int stripeId,
                                       @JsonProperty(value = "node", required = true) Node node) {
      super(cluster, cluster.getStripe(stripeId).get().getUID(), node);
    }
  }

}
