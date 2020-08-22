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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;

public class LockAwareDynamicConfigNomadChange implements DynamicConfigNomadChange {
  private final String lockToken;
  private final DynamicConfigNomadChange change;

  public LockAwareDynamicConfigNomadChange(String lockToken, DynamicConfigNomadChange change) {
    this.lockToken = lockToken;
    this.change = change;
  }

  @Override
  public Cluster apply(Cluster original) {
    return change.apply(original);
  }

  @Override
  public boolean canApplyAtRuntime(NodeContext currentNode) {
    throw new UnsupportedOperationException();
  }

  @JsonIgnore
  @Override
  public String getSummary() {
    return change.getSummary();
  }

  public String getLockToken() {
    return lockToken;
  }

  public DynamicConfigNomadChange getChange() {
    return change;
  }

  @Override
  public DynamicConfigNomadChange unwrap() {
    return change;
  }
}
