/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.dynamic_config.server.service.handler;

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.TopologyService;

import java.util.Collections;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class SystemPropertiesConfigChangeHandler implements ConfigChangeHandler {

  private final TopologyService topologyService;

  public SystemPropertiesConfigChangeHandler(TopologyService topologyService) {
    this.topologyService = requireNonNull(topologyService);
  }

  @Override
  public void apply(Configuration changes) {
    if (!changes.hasValue()) {
      // no value => erase
      if (changes.getKey() != null) {
        // removal of a single system property ?
        System.setProperty(changes.getKey(), "");
      } else {
        // removal of all tc system properties ?
        topologyService.getRuntimeNodeContext().getNode().getTcProperties()
          .orElse(Collections.emptyMap())
          .keySet()
          .forEach(key-> System.setProperty(key, ""));
      }

    } else {
      for (Configuration change : changes.expand()) {
        // About .get(): we have a value otherwise the config was not valid
        System.setProperty(change.getKey(), change.getValue().get());
      }
    }
  }
}
