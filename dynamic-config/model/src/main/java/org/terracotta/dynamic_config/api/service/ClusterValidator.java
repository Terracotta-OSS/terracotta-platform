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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Version;

/**
 * @author Mathieu Carbou
 */
public interface ClusterValidator {
  default void validate(Cluster cluster) throws MalformedClusterException {
    validate(cluster, Version.CURRENT);
  }

  void validate(Cluster cluster, Version version) throws MalformedClusterException;

  static ClusterValidator noop() {
    return (cluster, version) -> {
    };
  }
}
