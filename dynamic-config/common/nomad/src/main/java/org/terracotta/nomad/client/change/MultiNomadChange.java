/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Support applying multiple changes at once
 *
 * @author Mathieu Carbou
 */
@JsonTypeName("MultiNomadChange")
public class MultiNomadChange<T extends NomadChange> implements NomadChange {

  // keep this as a list, because the ordering to apply the changes might be important
  private final List<T> changes;

  @JsonCreator
  public MultiNomadChange(@JsonProperty(value = "changes", required = true) List<? extends T> changes) {
    this.changes = new ArrayList<>(requireNonNull(changes));
  }

  @SuppressWarnings("unchecked")
  public MultiNomadChange(T... changes) {
    this(Arrays.asList(changes));
  }

  public List<T> getChanges() {
    return changes;
  }

  @Override
  public String getSummary() {
    return changes.stream().map(NomadChange::getSummary).collect(Collectors.joining(" then "));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MultiNomadChange)) return false;
    MultiNomadChange<?> that = (MultiNomadChange<?>) o;
    return getChanges().equals(that.getChanges());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChanges());
  }

  @Override
  public String toString() {
    return getSummary();
  }
}
