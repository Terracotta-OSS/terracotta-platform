/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Objects;

public class SimpleNomadChange implements NomadChange {
  private final String change;
  private final String summary;

  @JsonCreator
  public SimpleNomadChange(@JsonProperty("change") String change, @JsonProperty("summary") String summary) {
    this.change = change;
    this.summary = summary;
  }

  public String getChange() {
    return change;
  }

  @Override
  public String getSummary() {
    return summary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimpleNomadChange that = (SimpleNomadChange) o;
    return Objects.equals(change, that.change) &&
        Objects.equals(summary, that.summary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(change, summary);
  }
}
