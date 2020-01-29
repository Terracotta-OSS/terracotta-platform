/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.change;

import java.util.ArrayList;
import java.util.List;

/**
 * A data change composed of several data changes in order.
 */
public class MuxSanskritChange implements SanskritChange {
  private final List<SanskritChange> changes;

  public MuxSanskritChange(List<SanskritChange> changes) {
    this.changes = new ArrayList<>(changes);
  }

  @Override
  public void accept(SanskritChangeVisitor visitor) {
    for (SanskritChange change : changes) {
      change.accept(visitor);
    }
  }
}
