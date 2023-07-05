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
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.NodeContext;

import static java.lang.String.format;

public class LockConfigNomadChange extends FilteredNomadChange {

  private final LockContext lockContext;

  // For Json
  LockConfigNomadChange() {
    lockContext = null;
  }

  public LockConfigNomadChange(LockContext lockContext) {
    super(Applicability.cluster());
    this.lockContext = lockContext;
  }

  @Override
  public Cluster apply(Cluster original) {
    Cluster updated = original.clone();
    updated.setConfigurationLockContext(lockContext);
    return updated;
  }

  @Override
  public boolean canUpdateRuntimeTopology(NodeContext nodeContext) {
    return true;
  }

  @Override
  public String getSummary() {
    return format("Locking the config by '%s'", lockContext.ownerInfo());
  }

  @Override
  public final String getType() {
    return "LockConfigNomadChange";
  }

  public LockContext getLockContext() {
    return lockContext;
  }
}
