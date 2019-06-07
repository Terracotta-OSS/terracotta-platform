/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.dynamic_config.model.Cluster;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * TODO [DYNAMIC-CONFIG]: DO WE NEED THIS NOMAD CHANGE ?
 * <p>
 * Nomad change that supports the initial setup of the config repository
 *
 * @author Mathieu Carbou
 */
public class ClusterActivationNomadChange extends FilteredNomadChange {

  private final String clusterName;
  private final Cluster cluster;

  @JsonCreator
  public ClusterActivationNomadChange(@JsonProperty("clusterName") String clusterName,
                                      @JsonProperty("cluster") Cluster cluster) {
    super(Applicability.cluster());
    this.clusterName = requireNonNull(clusterName);
    this.cluster = requireNonNull(cluster);
  }

  public Cluster getCluster() {
    return cluster;
  }

  public String getClusterName() {
    return clusterName;
  }

  @Override
  public String getSummary() {
    return "Activating cluster";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ClusterActivationNomadChange)) return false;
    if (!super.equals(o)) return false;
    ClusterActivationNomadChange that = (ClusterActivationNomadChange) o;
    return clusterName.equals(that.clusterName) &&
        getCluster().equals(that.getCluster());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), clusterName, getCluster());
  }
}
