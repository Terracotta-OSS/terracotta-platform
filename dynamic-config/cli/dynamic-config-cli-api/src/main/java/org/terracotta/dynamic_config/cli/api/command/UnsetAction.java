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
package org.terracotta.dynamic_config.cli.api.command;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Operation;

import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;

/**
 * @author Mathieu Carbou
 */
public class UnsetAction extends ConfigurationMutationAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(UnsetAction.class);

  public UnsetAction() {
    super(Operation.UNSET);
  }

  @Override
  protected void validate() {
    super.validate();

    // we support a list in case the user inputs: set -c license-file=foo/one.xml -c license-file=foo/two.xml
    // this is not illegal, would work, but a little stupid.
    // But could be useful in case the CLI is scripted and duplications happens (latter command overwrite previous ones)
    final List<org.terracotta.dynamic_config.api.model.Configuration> configs = configurations.stream()
        .filter(configuration -> configuration.getSetting() == LICENSE_FILE)
        .collect(Collectors.toList());

    // we remove the license parameters from the list of inputted commands
    // this will allow to update the license plus some configurations at the same time
    configurations.removeIf(cfg -> cfg.getSetting() == LICENSE_FILE);

    // Do we have some licence actions ?
    if (!configs.isEmpty()) {

      // take the last one in CLI
      final Configuration configuration = configs.get(configs.size() - 1);

      configuration.getValue().map(Paths::get).ifPresent(path -> {
        throw new IllegalArgumentException("Setting license-file must not be assigned to a value in unset command");
      });

      Collection<Node.Endpoint> peers = findRuntimePeers(node);
      LOGGER.debug("Uninstalling license from nodes: {}", toString(peers));
      upgradeLicense(peers, null);
      output.info("License removal successful.");
    }
  }
}
