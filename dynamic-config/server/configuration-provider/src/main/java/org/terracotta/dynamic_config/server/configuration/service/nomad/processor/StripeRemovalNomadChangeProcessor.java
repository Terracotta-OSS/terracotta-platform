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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.StripeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigListener;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

public class StripeRemovalNomadChangeProcessor implements NomadChangeProcessor<StripeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(StripeRemovalNomadChangeProcessor.class);

  private final TopologyService topologyService;
  private final DynamicConfigListener listener;

  public StripeRemovalNomadChangeProcessor(TopologyService topologyService, DynamicConfigListener listener) {
    this.topologyService = requireNonNull(topologyService);
    this.listener = requireNonNull(listener);
  }

  @Override
  public void validate(NodeContext baseConfig, StripeRemovalNomadChange change) throws NomadException {
    LOGGER.info("Validating change: {}", change.getSummary());
    if (baseConfig == null) {
      throw new NomadException("Existing config must not be null");
    }
    try {
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate();
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  @Override
  public void apply(StripeRemovalNomadChange change) throws NomadException {
    Cluster runtime = topologyService.getRuntimeNodeContext().getCluster();
    if (!runtime.getStripes().contains(change.getStripe())) {
      return;
    }

    try {
      LOGGER.info("Removing stripe: {} from cluster: {}", change.getStripe(), change.getCluster().getName());
      // TODO
      //listener.onNodeRemoval(change.getStripeId(), change.getNode());
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }
}
