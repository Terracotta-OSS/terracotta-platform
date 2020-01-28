/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.model.nomad;

import com.terracottatech.nomad.client.change.NomadChange;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public abstract class FilteredNomadChange implements NomadChange {

  protected final Applicability applicability;

  protected FilteredNomadChange(Applicability applicability) {
    this.applicability = requireNonNull(applicability);
  }

  public final Applicability getApplicability() {
    return applicability;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof FilteredNomadChange)) return false;
    FilteredNomadChange that = (FilteredNomadChange) o;
    return getApplicability().equals(that.getApplicability());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getApplicability());
  }
}
