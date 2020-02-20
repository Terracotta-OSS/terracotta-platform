/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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