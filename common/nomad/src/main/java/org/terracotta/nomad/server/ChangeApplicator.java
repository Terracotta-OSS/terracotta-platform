/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
