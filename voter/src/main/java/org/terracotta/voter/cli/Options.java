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
package org.terracotta.voter.cli;

import java.time.Duration;
import java.util.List;

public class Options {

  private boolean help;
  private String overrideHostPort;
  private List<String> serversHostPort;
  private Duration requestTimeout;
  private Duration connectionTimeout;
  private String connectionName = "Voter";

  public void setHelp(boolean help) {
    this.help = help;
  }

  public void setOverrideHostPort(String overrideHostPort) {
    this.overrideHostPort = overrideHostPort;
  }

  public void setServerHostPort(List<String> serversHostPort) {
    this.serversHostPort = serversHostPort;
  }

  public boolean isHelp() {
    return help;
  }

  public String getOverrideHostPort() {
    return overrideHostPort;
  }

  public List<String> getServersHostPort() {
    return serversHostPort;
  }

  public Duration getRequestTimeout() {
    return requestTimeout;
  }

  public void setRequestTimeout(Duration requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public Duration getConnectionTimeout() {
    return connectionTimeout;
  }

  public void setConnectionTimeout(Duration connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public String getConnectionName() {
    return connectionName;
  }

  public void setConnectionName(String connectionName) {
    this.connectionName = connectionName;
  }
}
