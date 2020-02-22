/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.server;

import org.terracotta.nomad.client.change.NomadChange;

import java.util.function.BiFunction;

public interface ChangeApplicator<T> {
  PotentialApplicationResult<T> tryApply(T existing, NomadChange change);

  void apply(NomadChange change) throws NomadException;

  /**
   * Change applicator that will always allow a change and will return a new
   * configuration computed by a function taking as parameter the change and
   * the previous configuration, which might be null at the beginning.
   */
  static <T> ChangeApplicator<T> allow(BiFunction<T, NomadChange, T> fn) {
    return new ChangeApplicator<T>() {
      @Override
      public PotentialApplicationResult<T> tryApply(T existing, NomadChange change) {
        return PotentialApplicationResult.allow(fn.apply(existing, change));
      }

      @Override
      public void apply(NomadChange change) throws NomadException {
      }
    };
  }
}
