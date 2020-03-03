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
package org.terracotta.dynamic_config.server.conversion;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
import org.terracotta.dynamic_config.server.nomad.UpgradableNomadServerFactory;
import org.terracotta.dynamic_config.server.nomad.persistence.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;
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

public class ConfigRepoProcessor implements PostConversionProcessor {
  private final Path outputFolderPath;
  private final NomadEnvironment nomadEnvironment;

  public ConfigRepoProcessor(Path outputFolderPath) {
    this.outputFolderPath = outputFolderPath;
    this.nomadEnvironment = new NomadEnvironment();
  }

  @Override
  public void process(Cluster cluster) {
    saveToNomad(cluster);
  }

  private void saveToNomad(Cluster cluster) {

    List<NomadEndpoint<NodeContext>> endpoints = cluster.nodeContexts()
        .map(nodeContext -> new NomadEndpoint<>(nodeContext.getNode().getNodeAddress(), getNomadServer(nodeContext.getStripeId(), nodeContext.getNodeName())))
        .collect(Collectors.toList());

    NomadEnvironment environment = new NomadEnvironment();
    NomadClient<NodeContext> nomadClient = new NomadClient<>(endpoints, environment.getHost(), environment.getUser(), Clock.systemUTC());
    NomadFailureReceiver<NodeContext> failureRecorder = new NomadFailureReceiver<>();
    nomadClient.tryApplyChange(failureRecorder, new ClusterActivationNomadChange(cluster));
    failureRecorder.reThrow();
  }

  protected NomadServer<NodeContext> getNomadServer(int stripeId, String nodeName) {
    Path repositoryPath = outputFolderPath.resolve("stripe" + stripeId + "_" + nodeName);
    return createServer(repositoryPath, stripeId, nodeName);
  }

  private NomadServer<NodeContext> createServer(Path repositoryPath, int stripeId, String nodeName) {
    ParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
    NomadRepositoryManager nomadRepositoryManager = new NomadRepositoryManager(repositoryPath, parameterSubstitutor);
    nomadRepositoryManager.createDirectories();

    ChangeApplicator<NodeContext> changeApplicator = new ChangeApplicator<NodeContext>() {
      @Override
      public PotentialApplicationResult<NodeContext> tryApply(final NodeContext existing, final NomadChange change) {
        return PotentialApplicationResult.allow(new NodeContext(
            ((ClusterActivationNomadChange) change).getCluster(),
            stripeId,
            nodeName
        ));
      }

      @Override
      public void apply(final NomadChange change) {
      }
    };

    try {
      return UpgradableNomadServerFactory.createServer(nomadRepositoryManager, changeApplicator, nodeName, parameterSubstitutor);
    } catch (SanskritException | NomadException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getUser() {
    return nomadEnvironment.getUser();
  }

  protected String getHost() {
    return nomadEnvironment.getHost();
  }
}