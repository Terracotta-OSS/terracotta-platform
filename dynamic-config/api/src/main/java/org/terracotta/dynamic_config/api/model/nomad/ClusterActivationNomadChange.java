/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports the initial setup of the config repository
 *
 * @author Mathieu Carbou
 */
@JsonTypeName("ClusterActivationNomadChange")
public class ClusterActivationNomadChange extends TopologyNomadChange {

  @JsonCreator
  public ClusterActivationNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster) {
    super(cluster, Applicability.cluster());
    requireNonNull(cluster.getName());
  }

  @Override
  public String getSummary() {
    return "Activating cluster";
  }

  @Override
  public Cluster apply(Cluster original) {
    return getCluster();
  }

  @Override
  public boolean canApplyAtRuntime() {
    return false;
  }

  @Override
  public String toString() {
    return "ClusterActivationNomadChange{" +
        "cluster=" + getCluster() +
        ", applicability=" + getApplicability() +
        '}';
  }
}
