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
 * Nomad change that supports the initial setup of the config repository
 *
 * @author Mathieu Carbou
 */
public class ClusterActivationNomadChange extends FilteredNomadChange {

  private final Cluster cluster;

  @JsonCreator
  public ClusterActivationNomadChange(@JsonProperty("cluster") Cluster cluster) {
    super(Applicability.cluster());
    this.cluster = requireNonNull(cluster);
    requireNonNull(cluster.getName());
  }

  public Cluster getCluster() {
    return cluster;
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
    return getCluster().equals(that.getCluster());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getCluster());
  }

  @Override
  public String toString() {
    return "ClusterActivationNomadChange{" +
        "cluster=" + cluster +
        ", applicability=" + applicability +
        '}';
  }
}
