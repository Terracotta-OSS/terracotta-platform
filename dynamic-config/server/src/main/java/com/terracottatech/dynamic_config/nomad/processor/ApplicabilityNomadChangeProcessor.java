/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.FilteredNomadChange;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.server.NomadException;

/**
 * Filters Nomad changes of type {@link FilteredNomadChange} based on their applicability
 */
public class ApplicabilityNomadChangeProcessor implements NomadChangeProcessor<NomadChange> {
  private final int stripeId;
  private final String nodeName;
  private final RoutingNomadChangeProcessor underlying;

  public ApplicabilityNomadChangeProcessor(int stripeId, String nodeName, RoutingNomadChangeProcessor nomadChangeProcessor) {
    this.stripeId = stripeId;
    this.nodeName = nodeName;
    this.underlying = nomadChangeProcessor;
  }

  @Override
  public NodeContext tryApply(NodeContext baseConfig, NomadChange change) throws NomadException {
    return applicableToThisServer(change) ? underlying.tryApply(baseConfig, change) : baseConfig;
  }

  @Override
  public void apply(NomadChange change) throws NomadException {
    if (applicableToThisServer(change)) {
      underlying.apply(change);
    }
  }

  private boolean applicableToThisServer(NomadChange change) {
    if (!(change instanceof FilteredNomadChange)) {
      return false;
    }
    Applicability applicability = ((FilteredNomadChange) change).getApplicability();
    switch (applicability.getType()) {
      case CLUSTER:
        return true;
      case STRIPE:
        return stripeId == applicability.getStripeId();
      case NODE:
        return stripeId == applicability.getStripeId() && nodeName.equals(applicability.getNodeName());
      default:
        throw new AssertionError("Unknown applicability: " + applicability);
    }
  }
}
