/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;

/**
 * @author Mathieu Carbou
 */
@JsonTypeName("ConfigMigrationNomadChange")
public class ConfigMigrationNomadChange extends TopologyNomadChange {

  @JsonCreator
  public ConfigMigrationNomadChange(@JsonProperty(value = "cluster", required = true) Cluster cluster) {
    super(cluster, Applicability.cluster());
  }

  @Override
  public String getSummary() {
    return "Migrating configuration";
  }

  @Override
  public String toString() {
    return "ConfigMigrationNomadChange{" +
        "cluster=" + getCluster() +
        ", applicability=" + getApplicability() +
        '}';
  }
}