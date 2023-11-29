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
package org.terracotta.healthchecker;

import org.terracotta.connection.entity.Entity;

import java.util.Timer;
import java.util.concurrent.Future;

interface HealthCheck extends Entity {
  long VERSION = 1;

  /**
   *  Ping a message to the server to make sure it is up and running
   * 
   * @param message The message to echo.
   * @return The Future to access the asynchronous response.
   */
   Future<String> ping(String message);
   
   Timer getTimer();
}
