/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.service.Props;

import java.util.Objects;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class Config {
  private final NodeContext topology;
  private final Version version;

  public Config(NodeContext topology, Version version) {
    this.topology = requireNonNull(topology);
    this.version = requireNonNull(version);
  }

  public NodeContext getTopology() {
    return topology;
  }

  public Version getVersion() {
    return version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Config)) return false;
    Config config = (Config) o;
    return topology.equals(config.topology) &&
        version == config.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(topology, version);
  }

  @Override
  public String toString() {
    Properties properties = topology.getStripe().toProperties(false, false, true, version);
    return "#Format Version: " + version + "\n" + Props.toString(properties);
  }
}
