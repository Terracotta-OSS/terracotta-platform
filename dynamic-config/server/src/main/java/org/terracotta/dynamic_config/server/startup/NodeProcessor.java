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
package org.terracotta.dynamic_config.server.startup;

import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.util.Map;

public class NodeProcessor {
  private final Options options;
  private final Map<Setting, String> paramValueMap;
  private final StartupManager startupManager;
  private final ClusterFactory clusterCreator;
  private final IParameterSubstitutor parameterSubstitutor;

  public NodeProcessor(Options options, Map<Setting, String> paramValueMap,
                       ClusterFactory clusterCreator,
                       StartupManager startupManager,
                       IParameterSubstitutor parameterSubstitutor) {
    this.options = options;
    this.paramValueMap = paramValueMap;
    this.clusterCreator = clusterCreator;
    this.startupManager = startupManager;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  public void process() {
    // Each NodeStarter either handles the startup itself or hands over to the next NodeStarter, following the chain-of-responsibility pattern
    NodeStarter third = new ConsoleParamsStarter(options, paramValueMap, clusterCreator, startupManager, parameterSubstitutor);
    NodeStarter second = new ConfigFileStarter(options, clusterCreator, startupManager, third);
    NodeStarter first = new ConfigRepoStarter(options, startupManager, second, parameterSubstitutor);
    boolean started = first.startNode();

    if (!started) {
      // If we're here, we've failed in our attempts to start the node
      throw new AssertionError("Exhausted all methods of starting the node. Giving up!");
    }
  }
}
