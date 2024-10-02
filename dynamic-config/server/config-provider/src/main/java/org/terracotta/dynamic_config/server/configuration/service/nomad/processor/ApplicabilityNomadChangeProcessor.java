/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.Applicability;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FilteredNomadChange;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.api.server.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

/**
 * Filters Nomad changes of type {@link FilteredNomadChange} based on their applicability
 */
public class ApplicabilityNomadChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange> {

  private final NomadChangeProcessor<DynamicConfigNomadChange> next;
  private final TopologyService topologyService;

  public ApplicabilityNomadChangeProcessor(TopologyService topologyService, NomadChangeProcessor<DynamicConfigNomadChange> nomadChangeProcessor) {
    this.topologyService = topologyService;
    this.next = nomadChangeProcessor;
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    if (applicableToThisServer(change)) {
      next.validate(baseConfig, change);
    }
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    if (applicableToThisServer(change)) {
      next.apply(change);
    }
  }

  private boolean applicableToThisServer(DynamicConfigNomadChange change) {
    if (!(change instanceof FilteredNomadChange)) {
      return false;
    }
    NodeContext nodeContext = topologyService.getRuntimeNodeContext();
    Applicability applicability = ((FilteredNomadChange) change).getApplicability();
    return applicability.isApplicableTo(nodeContext);
  }
}
