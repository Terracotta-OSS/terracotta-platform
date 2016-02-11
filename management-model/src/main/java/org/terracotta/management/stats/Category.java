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
package org.terracotta.management.stats;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public final class Category implements Serializable {

  private final String name;
  private final List<Statistic<?, ?>> statistics;

  public Category(String name, List<Statistic<?, ?>> statistics) {
    this.name = name;
    this.statistics = Collections.unmodifiableList(new ArrayList<Statistic<?, ?>>(statistics));
  }

  public String getName() {
    return name;
  }

  public List<Statistic<?, ?>> getStatistics() {
    return statistics;
  }
}
