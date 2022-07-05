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
package org.terracotta.dynamic_config.server.configuration.service.nomad.processor;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.dynamic_config.server.api.NomadRoutingChangeProcessor;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.server.NomadException;

import java.util.HashMap;
import java.util.Map;

/**
 * Routes the incoming Nomad change to the right processor based on the Nomad change type
 */
public class DefaultNomadRoutingChangeProcessor implements NomadChangeProcessor<DynamicConfigNomadChange>, NomadRoutingChangeProcessor {

  private final Map<Class<? extends NomadChange>, NomadChangeProcessor<DynamicConfigNomadChange>> processors = new HashMap<>();

  @Override
  @SuppressWarnings("unchecked")
  public <T extends DynamicConfigNomadChange> void register(Class<? extends T> changeType, NomadChangeProcessor<T> processor) {
    this.processors.put(changeType, (NomadChangeProcessor<DynamicConfigNomadChange>) processor);
  }

  @Override
  public void clear() {
    processors.clear();
  }

  @Override
  public void validate(NodeContext baseConfig, DynamicConfigNomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = getProcessor(change);
    processor.validate(baseConfig, change);
  }

  @Override
  public void apply(DynamicConfigNomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = getProcessor(change);
    processor.apply(change);
  }

  private NomadChangeProcessor<DynamicConfigNomadChange> getProcessor(NomadChange change) throws NomadException {
    NomadChangeProcessor<DynamicConfigNomadChange> processor = processors.get(change.getClass());

    if (processor == null) {
      throw new NomadException("Unknown change: " + change.getClass().getName());
    }

    return processor;
  }
}
