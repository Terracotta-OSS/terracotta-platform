/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.nomad.client.change.NomadChange;

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
@JsonTypeName("MultiSettingNomadChange")
public class MultiSettingNomadChange implements DynamicConfigNomadChange {

  // keep this as a list, because the ordering to apply the changes might be important
  private final List<SettingNomadChange> changes;

  @JsonCreator
  public MultiSettingNomadChange(@JsonProperty(value = "changes", required = true) List<SettingNomadChange> changes) {
    this.changes = new ArrayList<>(requireNonNull(changes));
  }

  public MultiSettingNomadChange(SettingNomadChange... changes) {
    this(Arrays.asList(changes));
  }

  public List<SettingNomadChange> getChanges() {
    return changes;
  }

  @Override
  public String getSummary() {
    return changes.stream().map(NomadChange::getSummary).collect(Collectors.joining(" then "));
  }

  @Override
  public Cluster apply(Cluster original) {
    for (DynamicConfigNomadChange change : changes) {
      original = change.apply(original);
    }
    return original;
  }

  @Override
  public boolean canApplyAtRuntime() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MultiSettingNomadChange)) return false;
    MultiSettingNomadChange that = (MultiSettingNomadChange) o;
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
