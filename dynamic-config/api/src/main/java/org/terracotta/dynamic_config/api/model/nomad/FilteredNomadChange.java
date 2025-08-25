/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.dynamic_config.api.model.nomad;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public abstract class FilteredNomadChange implements DynamicConfigNomadChange {

  private final Applicability applicability;

  // For Json
  FilteredNomadChange() {
    applicability = null;
  }

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
