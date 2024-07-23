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
package org.terracotta.nomad;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Clock;

public class NomadEnvironment {
  private static final String USER_NAME_PROPERTY = "user.name";
  private static final String UNKNOWN = "unknown";

  public String getHost() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      return UNKNOWN;
    }
  }

  public String getUser() {
    return System.getProperty(USER_NAME_PROPERTY, UNKNOWN);
  }

  public Clock getClock() {
    return Clock.systemUTC();
  }
}