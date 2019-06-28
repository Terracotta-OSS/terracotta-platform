/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

import static java.util.Objects.requireNonNull;


public class DynamicConfigServiceImpl implements DynamicConfigService {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigServiceImpl.class);

  private volatile Cluster cluster;
  private volatile Node me;

  public DynamicConfigServiceImpl(Cluster cluster, Node me) {
    this.cluster = requireNonNull(cluster);
    this.me = requireNonNull(me);
  }

  @Override
  public Node getThisNode() {
    return me;
  }

  @Override
  public InetSocketAddress getThisNodeAddress() {
    return me.getNodeAddress();
  }

  @Override
  public Cluster getTopology() {
    return cluster;
  }

  @Override
  public void setTopology(Cluster cluster) {
    this.cluster = requireNonNull(cluster);
    LOGGER.info("Set pending topology to: {}", cluster);
  }

  @Override
  public void prepareActivation(Cluster validatedTopology) {
    Node node = validatedTopology.getStripes()
        .stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .filter(node1 -> node1.getNodeHostname().equals(me.getNodeHostname()) && node1.getNodePort() == me.getNodePort())
        .findFirst()
        .orElseThrow(() -> {
          String message = String.format(
              "No match found for host: %s and port: %s in cluster topology: %s",
              me.getNodeHostname(),
              me.getNodePort(),
              validatedTopology
          );
          return new IllegalArgumentException(message);
        });

    LOGGER.info("Preparing activation of Node with validated topology: {}", validatedTopology);
    NomadBootstrapper.getNomadServerManager().upgradeForWrite(node.getNodeName(), node.getStripeName());
  }
}
