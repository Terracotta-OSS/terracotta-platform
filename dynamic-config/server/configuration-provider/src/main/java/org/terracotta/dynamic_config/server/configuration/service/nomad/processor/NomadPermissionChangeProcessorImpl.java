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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadPermissionChangeProcessor;
import org.terracotta.nomad.server.NomadException;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Global filter that will execute some validations regardless of the type of change
 *
 * @author Mathieu Carbou
 */
public class NomadPermissionChangeProcessorImpl implements NomadChangeProcessor<DynamicConfigNomadChange>, NomadPermissionChangeProcessor {

  private final List<Check> checks = new CopyOnWriteArrayList<>();

  private volatile NomadChangeProcessor<DynamicConfigNomadChange> next;

  public NomadPermissionChangeProcessorImpl then(NomadChangeProcessor<DynamicConfigNomadChange> next) {
    this.next = next;
    return this;
  }

  @Override
  public void addCheck(Check check) {
    if (checks.contains(check)) {
      throw new IllegalArgumentException("Check already exists: " + check);
    }
    checks.add(check);
  }

  @Override
  public void removeCheck(Check check) {
    checks.remove(check);
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    for (Check check : checks) {
      check.check(baseConfig, change);
    }
    if (next != null) {
      next.validate(baseConfig, change);
    }
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    if (next != null) {
      next.apply(change);
    }
  }
}
