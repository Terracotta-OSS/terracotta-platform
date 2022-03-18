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
package org.terracotta.dynamic_config.api.model;

public enum Scope {
  /**
   * Setting can be changed per node
   */
  NODE,

  /**
   * Setting can be changed for all nodes of a stripe
   */
  STRIPE,

  /**
   * Setting can be changed for all the nodes of a cluster
   */
  CLUSTER;


  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
