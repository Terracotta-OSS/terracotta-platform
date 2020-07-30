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
package org.terracotta.dynamic_config.api.model;

import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Objects;

import static java.util.Arrays.asList;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;
import static org.terracotta.dynamic_config.api.model.Operation.GET;
import static org.terracotta.dynamic_config.api.model.Operation.IMPORT;
import static org.terracotta.dynamic_config.api.model.Operation.SET;
import static org.terracotta.dynamic_config.api.model.Operation.UNSET;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

/**
 * @author Mathieu Carbou
 */
public class Permission {

  private final Collection<ClusterState> clusterStates;
  private final Collection<Operation> operations;
  private final Collection<Scope> levels;

  private Permission(Collection<ClusterState> clusterStates, Collection<Operation> operations, Collection<Scope> levels) {
    this.clusterStates = new HashSet<>(clusterStates);
    this.operations = new HashSet<>(operations);
    this.levels = new HashSet<>(levels);
  }

  public boolean allows(Scope scope) {
    return levels.contains(scope);
  }

  public boolean allows(Operation operation) {
    return operations.contains(operation);
  }

  public boolean allows(ClusterState clusterState) {
    return this.clusterStates.contains(clusterState);
  }

  public boolean isUserExportable() {
    return allows(CONFIGURING) && allows(GET);
  }

  public boolean isWritableWhen(ClusterState clusterState) {
    return allows(clusterState) && (allows(SET) || allows(UNSET) || allows(IMPORT));
  }

  @Override
  public String toString() {
    return "Permission: " +
        "when: " + clusterStates +
        " allow: " + operations +
        " at levels: " + levels;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Permission)) return false;
    Permission that = (Permission) o;
    return clusterStates.equals(that.clusterStates) &&
        operations.equals(that.operations) &&
        levels.equals(that.levels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clusterStates, operations, levels);
  }

  public static class Builder {

    private final Collection<ClusterState> clusterStates = new HashSet<>();
    private final Collection<Operation> operations = new HashSet<>();

    private Builder(Collection<ClusterState> clusterStates) {
      this.clusterStates.addAll(clusterStates);
    }

    public static Builder when(ClusterState... clusterStates) {
      return new Builder(asList(clusterStates));
    }

    public Builder allow(Operation... operations) {
      for (ClusterState clusterState : clusterStates) {
        for (Operation operation : operations) {
          if (!clusterState.supports(operation)) {
            throw new IllegalArgumentException("state: " + clusterState + " is not compatible with operation: " + operation);
          }
        }
      }
      this.operations.addAll(asList(operations));
      return this;
    }

    public Builder allowAnyOperations() {
      return allow(EnumSet.allOf(Operation.class).toArray(new Operation[0]));
    }

    public Permission atLevel(Scope level) {
      return atLevels(level);
    }

    public Permission atAnyLevels() {
      return atLevels(EnumSet.allOf(Scope.class).toArray(new Scope[0]));
    }

    public Permission atLevels(Scope... levels) {
      Collection<Scope> l = asList(levels);
      if (operations.contains(IMPORT) && (l.contains(STRIPE) || l.size() != 1)) {
        throw new IllegalArgumentException(IMPORT + " operation is only compatible with " + NODE + " or " + CLUSTER);
      }
      return new Permission(clusterStates, operations, l);
    }

  }
}
