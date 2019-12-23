/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.results.AllResultsReceiver;

public interface NomadDecider<T> extends AllResultsReceiver<T> {
  boolean isDiscoverSuccessful();

  boolean isWholeClusterAccepting();

  boolean isPrepareSuccessful();

  boolean isTakeoverSuccessful();

  boolean shouldDoCommit();

  default boolean shouldDoRollback() {
    return !shouldDoCommit();
  }

  Consistency getConsistency();
}
