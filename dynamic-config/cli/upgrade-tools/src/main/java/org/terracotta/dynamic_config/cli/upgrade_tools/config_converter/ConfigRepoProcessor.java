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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.ConfigStorageException;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.NomadClient;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.NomadFailureReceiver;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.PotentialApplicationResult;
import org.terracotta.persistence.sanskrit.SanskritException;

import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.stream.Collectors;

public class ConfigRepoProcessor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigRepoProcessor.class);

  private final Path outputFolderPath;
  private final NomadServerFactory nomadServerFactory = new NomadServerFactory(new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()));

  public ConfigRepoProcessor(Path outputFolderPath) {
    this.outputFolderPath = outputFolderPath;
  }

  public void process(Cluster cluster) {
    saveToNomad(cluster);
  }

  private void saveToNomad(Cluster cluster) {

    List<NomadEndpoint<NodeContext>> endpoints = cluster.getNodes().stream()
        .map(node -> new NomadEndpoint<>(node.getInternalAddress(), getNomadServer(cluster, node)))
        .collect(Collectors.toList());

    NomadEnvironment environment = new NomadEnvironment();
    try (NomadClient<NodeContext> nomadClient = new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC())) {
      NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
      nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
      failureRecorder.reThrowErrors();
    }
  }

  protected NomadServer<NodeContext> getNomadServer(Cluster cluster, Node node) {
    int stripeId = cluster.getStripeIdByNode(node.getUID()).getAsInt();
    Stripe stripe = cluster.getStripeByNode(node.getUID()).get();
    Path configPath = outputFolderPath.resolve("stripe-" + stripeId).resolve(node.getName());
    LOGGER.info("Preparing configuration folder for node: " + node.getName() + " in stripe: " + stripe.getName() + " at: " + configPath.toAbsolutePath());
    return createServer(configPath, node);
  }

  private NomadServer<NodeContext> createServer(Path configPath, Node node) {
    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(configPath, IParameterSubstitutor.identity());
    nomadConfigurationManager.createDirectories();

    ChangeApplicator<NodeContext> changeApplicator = new ChangeApplicator<NodeContext>() {
      @Override
      public PotentialApplicationResult<NodeContext> tryApply(final NodeContext existing, final NomadChange change) {
        return PotentialApplicationResult.allow(new NodeContext(((ClusterActivationNomadChange) change).getCluster(), node.getUID()
        ));
      }

      @Override
      public void apply(final NomadChange change) {
      }
    };

    try {
      return nomadServerFactory.createServer(nomadConfigurationManager, changeApplicator, node.getName(), null);
    } catch (SanskritException | NomadException | ConfigStorageException e) {
      throw new RuntimeException(e);
    }
  }
}