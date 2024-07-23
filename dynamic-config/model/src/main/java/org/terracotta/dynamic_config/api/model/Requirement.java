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
package org.terracotta.dynamic_config.api.model;

/**
 * @author Mathieu Carbou
 */
public enum Requirement {

  /**
   * Setting change needs a restart of all the nodes.
   * APPLY TO CLUSTER-WIDE SETTING ONLY
   */
  CLUSTER_RESTART,

  /**
   * Setting change needs all nodes online (active and passives).
   * APPLY TO CLUSTER-WIDE SETTING ONLY
   */
  CLUSTER_ONLINE,

  /**
   * Setting change needs a restart of only the impacted nodes.
   * APPLY TO NODE SETTING ONLY
   */
  NODE_RESTART,

  /**
   * A setting that must be eagerly resolved (placeholders) on server-side as soon as possible before any configuration parsing.
   * Settings requiring that are those used to identify nodes such as hostname and port.
   */
  RESOLVE_EAGERLY,

  /**
   * A setting that must be set by the user or which must have a default because the presence of a value is required at runtime
   */
  PRESENCE,

  /**
   * A setting that requires the user to provide a value for it in the configuration or CLI. It cannot be left blank.
   */
  CONFIG,

  /**
   * A system setting that is hidden from the user but should be persisted and reloading in the configuration repository
   */
  HIDDEN
}
