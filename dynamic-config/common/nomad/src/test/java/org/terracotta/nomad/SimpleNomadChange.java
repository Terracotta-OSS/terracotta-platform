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
package org.terracotta.nomad;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.terracotta.nomad.client.change.NomadChange;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

@JsonTypeName("SimpleNomadChange")
public class SimpleNomadChange implements NomadChange {
  private final String change;
  private final String summary;

  @JsonCreator
  public SimpleNomadChange(@JsonProperty(value = "change", required = true) String change,
                           @JsonProperty(value = "summary", required = true) String summary) {
    this.change = requireNonNull(change);
    this.summary = requireNonNull(summary);
  }

  public String getChange() {
    return change;
  }

  @Override
  public String getSummary() {
    return summary;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SimpleNomadChange that = (SimpleNomadChange) o;
    return Objects.equals(change, that.change) &&
        Objects.equals(summary, that.summary);
  }

  @Override
  public int hashCode() {
    return Objects.hash(change, summary);
  }
}
