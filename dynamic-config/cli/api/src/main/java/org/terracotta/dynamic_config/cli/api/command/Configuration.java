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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.cli.api.output.OutputService;

public class Configuration {

  private Measure<TimeUnit> entityOperationTimeout;
  private Measure<TimeUnit> requestTimeout = Measure.of(10, TimeUnit.SECONDS);
  private Measure<TimeUnit> connectionTimeout = Measure.of(10, TimeUnit.SECONDS);
  private String securityRootDirectory;
  private String lockToken;
  private OutputService outputService;

  public Configuration(OutputService outputService) {
    this.outputService = outputService;
  }

  public OutputService getOutputService() {
    return outputService;
  }

  public void setOutputService(OutputService outputService) {
    this.outputService = outputService;
  }

  public void setEntityOperationTimeout(Measure<TimeUnit> entityOperationTimeout) {
    this.entityOperationTimeout = entityOperationTimeout;
  }

  public void setRequestTimeout(Measure<TimeUnit> requestTimeout) {
    this.requestTimeout = requestTimeout;
  }

  public void setConnectionTimeout(Measure<TimeUnit> connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
  }

  public void setSecurityDirectory(String securityRootDirectory) {
    this.securityRootDirectory = securityRootDirectory;
  }

  public void setLockToken(String lockToken) {
    this.lockToken = lockToken;
  }

  public Measure<TimeUnit> getRequestTimeout() {
    return requestTimeout;
  }

  public Measure<TimeUnit> getConnectionTimeout() {
    return connectionTimeout;
  }

  public Measure<TimeUnit> getEntityOperationTimeout() {
    return entityOperationTimeout;
  }

  public String getSecurityRootDirectory() {
    return securityRootDirectory;
  }

  public String getLockToken() {
    return lockToken;
  }
}
