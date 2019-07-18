/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.diagnostic;

import com.tc.server.TCServerMain;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.monitoring.PlatformService;

import java.net.InetSocketAddress;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class TopologyServiceImpl implements TopologyService {
  private static final Logger LOGGER = LoggerFactory.getLogger(TopologyServiceImpl.class);

  private volatile Cluster cluster;
  private volatile Node me;
  private final boolean clusterActivated;

  public TopologyServiceImpl(Cluster cluster, Node me, boolean clusterActivated) {
    this.cluster = requireNonNull(cluster);
    this.me = requireNonNull(me);
    this.clusterActivated = clusterActivated;
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
  public void restart() {
    LOGGER.info("Executing restart on node: {} in stripe: {}", me.getNodeName(), cluster.getStripeId(me).get());
    TCServerMain.getServer().stop(PlatformService.RestartMode.STOP_AND_RESTART);
  }

  @Override
  public Cluster getTopology() {
    return cluster;
  }

  @Override
  public boolean isActivated() {
    return clusterActivated;
  }

  @Override
  public void setTopology(Cluster cluster) {
    requireNonNull(cluster);

    if (isActivated()) {
      throw new UnsupportedOperationException("Unable to change the topology at runtime");

    } else {
      Optional<Node> me = cluster.getNode(this.me.getNodeAddress());
      if (me.isPresent()) {
        // we have updated the topology and I am still part of this cluster
        LOGGER.info("Set pending topology to: {}", cluster);
        this.cluster = cluster;
        this.me = me.get();
      } else {
        // We have updated the topology and I am not part anymore of the cluster
        // So we just reset the cluster object so that this node is alone
        LOGGER.info("Node {} removed from pending topology: {}", this.me.getNodeAddress(), cluster);
        this.cluster = new Cluster(new Stripe(this.me));
      }

    }
  }

  @Override
  public void prepareActivation(Cluster validatedTopology) {
    if (isActivated()) {
      throw new IllegalStateException("Node is already activated");
    }
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
    NomadBootstrapper.getNomadServerManager().upgradeForWrite(node.getNodeName(), validatedTopology.getStripeId(node).get());
  }
}
