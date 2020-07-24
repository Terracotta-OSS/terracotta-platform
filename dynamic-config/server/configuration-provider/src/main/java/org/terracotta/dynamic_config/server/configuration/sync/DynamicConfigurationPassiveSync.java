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
package org.terracotta.dynamic_config.server.configuration.sync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.DynamicConfigService;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public class DynamicConfigurationPassiveSync {

  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicConfigurationPassiveSync.class);

  private final UpgradableNomadServer<NodeContext> nomadServer;
  private final DynamicConfigService dynamicConfigService;
  private final Supplier<String> licenseContent;
  private final DynamicConfigNomadSynchronizer nomadSynchronizer;

  public DynamicConfigurationPassiveSync(NodeContext nodeStartupConfiguration,
                                         UpgradableNomadServer<NodeContext> nomadServer,
                                         DynamicConfigService dynamicConfigService,
                                         Supplier<String> licenseContent) {
    this.nomadServer = nomadServer;
    this.dynamicConfigService = dynamicConfigService;
    this.licenseContent = licenseContent;
    this.nomadSynchronizer = new DynamicConfigNomadSynchronizer(nodeStartupConfiguration, nomadServer);
  }

  public DynamicConfigSyncData getSyncData() {
    try {
      return new DynamicConfigSyncData(nomadServer.getAllNomadChanges(), licenseContent.get());
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Nomad changes:
   * - SettingNomadChange (config change)
   * - MultiSettingNomadChange (contains several changes at once)
   * - TopologyNomadChange (activation, node addition, node removal)
   * <p>
   * Nomad processors are executed in this order:
   * - ConfigChangeApplicator: unwrap the MultiNomadChange, returns the new config if change is allowed
   * - ApplicabilityNomadChangeProcessor: only call following processors if stripe/node/cluster matches
   * - RoutingNomadChangeProcessor: call the right processor depending on the message type
   * <p>
   * 2 Kind of processors:
   * - SettingNomadChangeProcessor: handle config changes
   * - TopologyNomadChangeProcessor (activation, node addition, node removal): will update the config, or create a new one
   * <p>
   * Sync process from active A to passive P:
   * - Needs to sync the active append log into the passive one
   * - We want all the nodes to have the same append log
   * - We want to sync append log history from A that do not relate to P, without triggering the processors of P
   * - We want to reset and re-sync P append log when:
   * .... 1) P has been started pre-activated to join a pre-activated stripe
   * .... 2) P has been activated and restarted as passive as part of node addition
   */
  public Set<Require> sync(DynamicConfigSyncData data) throws NomadException {
    // sync the active append log in the passive append log
    Set<Require> requires = nomadSynchronizer.syncNomadChanges(data.getNomadChanges());

    // sync the license from active node
    syncLicense(data.getLicense());

    return requires;
  }

  private void syncLicense(String activeLicense) {
    String passiveLicense = licenseContent.get();
    if (!Objects.equals(activeLicense, passiveLicense)) {
      LOGGER.info("Syncing license");
      dynamicConfigService.upgradeLicense(activeLicense);
    }
  }
}
