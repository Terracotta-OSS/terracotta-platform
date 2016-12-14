/*
 * Copyright Terracotta, Inc.
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
package org.terracotta.management.entity.sample;

import org.terracotta.voltron.proxy.ConcurrencyStrategy;
import org.terracotta.voltron.proxy.ExecutionStrategy;

import static org.terracotta.voltron.proxy.ConcurrencyStrategy.*;
import static org.terracotta.voltron.proxy.ExecutionStrategy.Location.ACTIVE;
import static org.terracotta.voltron.proxy.ExecutionStrategy.Location.BOTH;

/**
 * @author Mathieu Carbou
 */
public interface Cache {

  // The answer to life, the universe and everything
  int MUTATION_KEY = 42;

  // reads

  @ConcurrencyStrategy(key = UNIVERSAL_KEY)
  @ExecutionStrategy(location = ACTIVE)
  String get(String key);

  /**
   * reports current's layer heap
   */
  @ConcurrencyStrategy(key = UNIVERSAL_KEY)
  @ExecutionStrategy(location = ACTIVE)
  int size();

  // mutations

  @ConcurrencyStrategy(key = MUTATION_KEY)
  @ExecutionStrategy(location = BOTH)
  void put(String key, String value);

  /**
   * removes a key, and fire a message to clients to do the same
   */
  @ConcurrencyStrategy(key = MUTATION_KEY)
  @ExecutionStrategy(location = BOTH)
  void remove(String key);

  /**
   * empty current layer's heap, and fire a message to clients to do the same
   */
  @ConcurrencyStrategy(key = MUTATION_KEY)
  @ExecutionStrategy(location = BOTH)
  void clear();

}
