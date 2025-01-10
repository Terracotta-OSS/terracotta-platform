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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ConfigFormat;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.api.model.Scope;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class GetAction extends ConfigurationAction {

  private boolean wantsRuntimeConfig;
  private ConfigFormat outputFormat = ConfigFormat.CONFIG;

  public GetAction() {
    super(Operation.GET);
  }

  public void setOutputFormat(ConfigFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  public void setRuntimeConfig(boolean wantsRuntimeConfig) {
    this.wantsRuntimeConfig = wantsRuntimeConfig;
  }

  @Override
  public void run() {
    super.validate();
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    Map<String, String> properties = new TreeMap<>();
    // we put both expanded and non-expanded properties,
    // and we will filter depending on what the user wanted
    cluster.toProperties(false, true, true).forEach((k, v) -> properties.put((String) k, (String) v));
    cluster.toProperties(true, true, true).forEach((k, v) -> properties.put((String) k, (String) v));

    // for each configuration asked by the user we try to find it
    for (Configuration configuration : configurations) {
      List<String> out = properties.entrySet()
          .stream()
          .filter(e -> configuration.matchConfigPropertyKey(e.getKey()))
          .map(e -> formatOutput(e.getKey(), e.getValue(), cluster))
          .collect(Collectors.toList());
      if (!out.isEmpty()) {
        out.forEach(s -> output.out(s));
      }
    }
  }

  private String formatOutput(String key, String value, Cluster cluster) {
    StringBuilder sb = new StringBuilder();
    switch (outputFormat) {

      case PROPERTIES:
        // key is: [stripe.x.[node.y.]]<setting>[.<key>]
        sb.append(key);
        break;

      case CONFIG:
        // will become: (stripe|node):<node_or_stripe_name>:<setting>[.<key>]
        Configuration c = Configuration.valueOf(key);
        if (c.getLevel() == Scope.STRIPE) {
          sb.append("stripe:").append(cluster.getStripe(c.getStripeId()).get().getName()).append(":");
        } else if (c.getLevel() == Scope.NODE) {
          sb.append("node:").append(cluster.getStripe(c.getStripeId()).get().getNodes().get(c.getNodeId() - 1).getName()).append(":");
        }
        sb.append(c.getSetting());
        if (c.getKey() != null) {
          sb.append(".").append(c.getKey());
        }
        break;

      default:
        throw new IllegalArgumentException("Invalid format: " + outputFormat + ". Supported formats: " + String.join(", ", ConfigFormat.supported()));
    }
    if (outputFormat == ConfigFormat.PROPERTIES) {

    } else {

    }
    return sb.append("=").append(value).toString();
  }
}
