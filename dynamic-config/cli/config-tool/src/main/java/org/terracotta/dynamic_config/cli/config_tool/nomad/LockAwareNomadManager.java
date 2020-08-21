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
package org.terracotta.dynamic_config.cli.config_tool.nomad;

import org.terracotta.diagnostic.client.connection.MultiDiagnosticServiceProvider;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.LockAwareDynamicConfigNomadChange;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.entity.client.NomadEntityProvider;

public class LockAwareNomadManager<T> extends NomadManager<T> {
  private final String lockToken;

  public LockAwareNomadManager(NomadEnvironment environment, MultiDiagnosticServiceProvider<UID> multiDiagnosticServiceProvider, NomadEntityProvider nomadEntityProvider, String lockToken) {
    super(environment, multiDiagnosticServiceProvider, nomadEntityProvider);
    this.lockToken = lockToken;
  }

  @Override
  protected DynamicConfigNomadChange wrapNomadChange(DynamicConfigNomadChange change) {
    return new LockAwareDynamicConfigNomadChange(lockToken, change);
  }
}
