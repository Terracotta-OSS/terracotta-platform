/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public abstract class TopologyNomadChange extends FilteredNomadChange {

  private final Cluster cluster;

  protected TopologyNomadChange(Cluster cluster, Applicability applicability) {
    super(applicability);
    this.cluster = requireNonNull(cluster);
  }

  public final Cluster getCluster() {
    return cluster;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof TopologyNomadChange)) return false;
    if (!super.equals(o)) return false;
    TopologyNomadChange that = (TopologyNomadChange) o;
    return getCluster().equals(that.getCluster());
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), getCluster());
  }
}
