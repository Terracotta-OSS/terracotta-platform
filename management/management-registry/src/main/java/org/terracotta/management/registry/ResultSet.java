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
package org.terracotta.management.registry;

import org.terracotta.management.model.context.Context;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * @param <T> The result type ({@link org.terracotta.management.model.stats.ContextualStatistics}, {@link org.terracotta.management.model.call.ContextualReturn})
 *
 * @author Mathieu Carbou
 */
public interface ResultSet<T> extends Iterable<T> {

  /**
   * @return The result of the query for a specific context
   */
  T getResult(Context context);

  Map<Context, T> results();

  /**
   * @return The result of the query for the only one existing context
   * @throws NoSuchElementException If the result set is not executed or has more than 1 values
   */
  T getSingleResult() throws NoSuchElementException;

  int size();

  boolean isEmpty();
}
