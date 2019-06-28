/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracottatech.nomad.client.change.NomadChange;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * Nomad change that supports repairing configuration
 *
 * @author Mathieu Carbou
 */
public class ConfigRepairNomadChange implements NomadChange {

  private final String configuration;

  @JsonCreator
  public ConfigRepairNomadChange(@JsonProperty("configuration") String configuration) {
    this.configuration = requireNonNull(configuration);
  }

  public String getConfiguration() {
    return configuration;
  }

  @Override
  public String getSummary() {
    return "Repairing configuration";
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof ConfigRepairNomadChange)) return false;
    ConfigRepairNomadChange that = (ConfigRepairNomadChange) o;
    return getConfiguration().equals(that.getConfiguration());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getConfiguration());
  }

  @Override
  public String toString() {
    return "ConfigRepairNomadChange{" +
        "configuration='" + configuration + '\'' +
        '}';
  }
}
