/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.healthchecker;

import java.util.Timer;
import java.util.concurrent.Future;
import org.terracotta.connection.entity.Entity;

interface HealthCheck extends Entity {
  static final long VERSION = 1;

  /**
   *  Ping a message to the server to make sure it is up and running
   * 
   * @param message The message to echo.
   * @return The Future to access the asynchronous response.
   */
   Future<String> ping(String message);
   
   Timer getTimer();
}
