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

public class InvalidConfigChangeException extends Exception {
  private static final long serialVersionUID = -3752283156707939955L;

  public InvalidConfigChangeException(String message) {
    super(message);
  }

  public InvalidConfigChangeException(String message, Throwable cause) {
    super(message, cause);
  }
}
