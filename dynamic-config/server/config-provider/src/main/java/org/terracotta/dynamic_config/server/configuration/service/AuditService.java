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
package org.terracotta.dynamic_config.server.configuration.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.dynamic_config.api.service.NomadChangeInfo;
import org.terracotta.server.Server;

import java.time.Duration;
import java.util.Optional;
import java.util.Properties;

public class AuditService implements DynamicConfigService {
  private final DynamicConfigService dynamicConfigService;
  private final Server server;

  public AuditService(DynamicConfigService dynamicConfigService, Server server) {
    this.dynamicConfigService = dynamicConfigService;
    this.server = server;
  }

  @Override
  public Optional<String> getLicenseContent() {
    server.audit("License retrieval requested", new Properties());
    return dynamicConfigService.getLicenseContent();
  }

  @Override
  public void resetAndSync(NomadChangeInfo[] nomadChanges, Cluster cluster) {
    server.audit("Reset and sync invoked", new Properties());
    dynamicConfigService.resetAndSync(nomadChanges, cluster);
  }

  @Override
  public void setUpcomingCluster(Cluster cluster) {
    server.audit("Setting upcoming cluster to " + cluster.toProperties(false, false, true), new Properties());
    dynamicConfigService.setUpcomingCluster(cluster);
  }

  @Override
  public void activate(Cluster maybeUpdatedCluster, String licenseContent) {
    server.audit("Activate invoked", new Properties());
    dynamicConfigService.activate(maybeUpdatedCluster, licenseContent);
  }

  @Override
  public void reset() {
    server.audit("Reset invoked", new Properties());
    dynamicConfigService.reset();
  }

  @Override
  public void restart(Duration delayInSeconds) {
    server.audit("Restart invoked", new Properties());
    dynamicConfigService.restart(delayInSeconds);
  }

  @Override
  public void restartIfPassive(Duration delay) {
    server.audit("RestartIfPassive invoked", new Properties());
    dynamicConfigService.restartIfPassive(delay);
  }

  @Override
  public void restartIfActive(Duration delay) {
    server.audit("RestartIfActive invoked", new Properties());
    dynamicConfigService.restartIfActive(delay);
  }

  @Override
  public void stop(Duration delayInSeconds) {
    server.audit("Stop invoked", new Properties());
    dynamicConfigService.stop(delayInSeconds);
  }

  @Override
  public void upgradeLicense(String licenseContent) {
    server.audit("Upgrade license invoked", new Properties());
    dynamicConfigService.upgradeLicense(licenseContent);
  }
}
