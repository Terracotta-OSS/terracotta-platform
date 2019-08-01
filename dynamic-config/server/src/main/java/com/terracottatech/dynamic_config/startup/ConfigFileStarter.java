/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

package com.terracottatech.dynamic_config.startup;

import com.terracottatech.dynamic_config.diagnostic.LicensingService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.parsing.Options;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_HOSTNAME;
import static com.terracottatech.dynamic_config.DynamicConfigConstants.DEFAULT_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.util.ParameterSubstitutor.substitute;
import static java.util.Objects.requireNonNull;

public class ConfigFileStarter extends NodeStarter {
  private final Options options;
  private final LicensingService licensingService;
  private final NodeStarter nextStarter;

  ConfigFileStarter(Options options, LicensingService licensingService, NodeStarter nextStarter) {
    this.options = options;
    this.licensingService = licensingService;
    this.nextStarter = nextStarter;
  }

  @Override
  public void startNode(Cluster cluster, Node node) {
    if (options.getConfigFile() == null) {
      nextStarter.startNode(cluster, node);
    }

    Path substitutedConfigFile = Paths.get(substitute(options.getConfigFile()));
    logger.info("Starting node from config file: {}", substitutedConfigFile);
    cluster = ClusterCreator.createCluster(substitutedConfigFile, options.getClusterName());
    node = getMatchingNodeFromConfigFile(cluster);

    if (options.getLicenseFile() != null) {
      if (cluster.getNodeCount() > 1) {
        //TODO [DYNAMIC-CONFIG] TRACK #6: relax this constraint
        throw new UnsupportedOperationException("License file option can be used only with a one-node cluster config file");
      }
      startPreactivated(cluster, node, licensingService, options.getLicenseFile());
    } else {
      startUnconfigured(cluster, node);
    }
  }

  private Node getMatchingNodeFromConfigFile(Cluster cluster) {
    boolean isHostnameSpecified = options.getNodeHostname() != null;
    boolean isPortSpecified = options.getNodePort() != null;

    String substitutedHost = substitute(isHostnameSpecified ? options.getNodeHostname() : DEFAULT_HOSTNAME);
    String port = isPortSpecified ? options.getNodePort() : DEFAULT_PORT;

    List<Node> allNodes = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .collect(Collectors.toList());
    Optional<Node> matchingNodeOptional = allNodes.stream()
        .filter(node1 -> substitute(node1.getNodeHostname()).equals(substitutedHost) && node1.getNodePort() == Integer.parseInt(port))
        .findAny();

    Node node;
    // See if we find a match for a node based on the specified params. If not, we see if the config file contains just one node
    if (matchingNodeOptional.isPresent()) {
      logger.info("Found matching node entry from config file based on {}={} and {}={}", NODE_HOSTNAME, substitutedHost, NODE_PORT, port);
      node = matchingNodeOptional.get();
    } else if (!isHostnameSpecified && !isPortSpecified && allNodes.size() == 1) {
      logger.info("Found only one node information in config file");
      node = allNodes.get(0);
    } else {
      throw new RuntimeException(
          String.format(
              "Did not find a matching node entry in config file: %s based on %s=%s and %s=%s",
              requireNonNull(substitute(options.getConfigFile())),
              NODE_HOSTNAME,
              substitutedHost,
              NODE_PORT,
              port
          )
      );
    }
    return node;
  }
}
