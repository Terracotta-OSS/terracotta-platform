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
package org.terracotta.dynamic_config.server.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.FilteredNomadChange;
import org.terracotta.dynamic_config.api.model.nomad.MultiSettingNomadChange;
import org.terracotta.nomad.server.NomadException;

import java.util.List;

/**
 * Filters Nomad changes of type {@link FilteredNomadChange} based on their applicability
 */
public class MultiSettingNomadChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange> {
  private final NomadChangeProcessor<DynamicConfigNomadChange> next;

  public MultiSettingNomadChangeProcessor(NomadChangeProcessor<DynamicConfigNomadChange> next) {
    this.next = next;
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    List<? extends DynamicConfigNomadChange> changes = MultiSettingNomadChange.extractChanges(change);
    for (DynamicConfigNomadChange dynamicConfigNomadChange : changes) {
      next.validate(baseConfig, dynamicConfigNomadChange);
      if (baseConfig != null) {
        baseConfig = baseConfig.withCluster(dynamicConfigNomadChange.apply(baseConfig.getCluster()));
      }
    }
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    List<? extends DynamicConfigNomadChange> changes = MultiSettingNomadChange.extractChanges(change);
    for (DynamicConfigNomadChange dynamicConfigNomadChange : changes) {
      next.apply(dynamicConfigNomadChange);
    }
  }
}
