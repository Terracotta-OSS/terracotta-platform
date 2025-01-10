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
package org.terracotta.dynamic_config.api.server;

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;

/**
 * Handles config changes on the server side
 */
public interface ConfigChangeHandler {

  /**
   * Validate a change and throw if invalid
   */
  default void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {}


  /**
   * Apply a change at runtime on the server
   */
  default void apply(Configuration change) {
  }

  /**
   * Handler that will return null to reject a change
   */
  static ConfigChangeHandler reject(String reason) {
    return new ConfigChangeHandler() {
      @Override
      public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
        throw new InvalidConfigChangeException(reason);
      }

      @Override
      public String toString() {
        return "ConfigChangeHandler#reject()";
      }
    };
  }

  /**
   * Handler that will just apply the change after a restart
   */
  static ConfigChangeHandler accept() {
    return new ConfigChangeHandler() {
      @Override
      public String toString() {
        return "ConfigChangeHandler#accept()";
      }
    };
  }
}
