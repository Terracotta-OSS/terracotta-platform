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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class GetAction extends ConfigurationAction {

  private boolean wantsRuntimeConfig;

  public GetAction() {
    super(Operation.GET);
  }

  public void setRuntimConfig(boolean wantsRuntimeConfig) {
    this.wantsRuntimeConfig = wantsRuntimeConfig;
  }

  @Override
  public void run() {
    super.validate();
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    Map<String, String> properties = new TreeMap<>();
    // we put both expanded and non expanded properties
    // and we will filter depending on what the user wanted
    cluster.toProperties(false, true, true).forEach((k, v) -> properties.put((String) k, (String) v));
    cluster.toProperties(true, true, true).forEach((k, v) -> properties.put((String) k, (String) v));

    // for each configuration asked by the user we try to find it
    for (Configuration configuration : configurations) {
      List<String> out = properties.entrySet()
          .stream()
          .filter(e -> configuration.matchConfigPropertyKey(e.getKey()))
          .map(e -> e.getKey() + "=" + e.getValue())
          .collect(Collectors.toList());
      if (!out.isEmpty()) {
        out.forEach(s -> output.out(s));
      }
    }
  }
}
