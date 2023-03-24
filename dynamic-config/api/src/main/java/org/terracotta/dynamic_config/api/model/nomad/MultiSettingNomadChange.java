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
package org.terracotta.dynamic_config.api.model.nomad;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.nomad.client.change.NomadChange;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * Support applying multiple changes at once
 *
 * @author Mathieu Carbou
 */
public class MultiSettingNomadChange implements DynamicConfigNomadChange {

  // keep this as a list, because the ordering to apply the changes might be important
  private final List<SettingNomadChange> changes;

  public MultiSettingNomadChange(List<SettingNomadChange> changes) {
    this.changes = new ArrayList<>(requireNonNull(changes));
  }

  public MultiSettingNomadChange(SettingNomadChange... changes) {
    this(Arrays.asList(changes));
  }

  public List<SettingNomadChange> getChanges() {
    return changes;
  }

  @Override
  public String getSummary() {
    return changes.stream().map(NomadChange::getSummary).collect(Collectors.joining(" then "));
  }

  @Override
  public final String getType() {
    return "MultiSettingNomadChange";
  }

  @Override
  public Cluster apply(Cluster original) {
    for (DynamicConfigNomadChange change : changes) {
      original = change.apply(original);
    }
    return original;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext currentNode) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof MultiSettingNomadChange)) return false;
    MultiSettingNomadChange that = (MultiSettingNomadChange) o;
    return getChanges().equals(that.getChanges());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChanges());
  }

  @Override
  public String toString() {
    return getSummary();
  }

  public static List<? extends DynamicConfigNomadChange> extractChanges(DynamicConfigNomadChange change) {
    return change instanceof MultiSettingNomadChange ? ((MultiSettingNomadChange) change).getChanges() : Collections.singletonList(change);
  }
}
