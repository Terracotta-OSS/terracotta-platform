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
package org.terracotta.management.model.stats;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public interface Statistic<T extends Serializable> extends Serializable {
  StatisticType getType();

  boolean isEmpty();

  List<Sample<T>> getSamples();

  Optional<T> getLatestSampleValue();

  Optional<Sample<T>> getLatestSample();

  @Override
  String toString();
}
