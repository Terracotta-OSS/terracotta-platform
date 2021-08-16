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

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;

public class SetAction extends ConfigurationMutationAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(SetAction.class);

  public SetAction() {
    super(Operation.SET);
  }

  @Override
  protected void validate() {
    super.validate();

    // we support a list in case the user inputs: set -c license-file=foo/one.xml -c license-file=foo/two.xml
    // this is not illegal, would work, but a little stupid.
    // But could be useful in case the CLI is scripted and duplications happens (latter command overwrite previous ones)
    final List<Configuration> configs = configurations.stream()
        .filter(configuration -> configuration.getSetting() == LICENSE_FILE)
        .collect(toList());

    // we remove the license parameters from the list of inputted commands
    // this will allow to update the license plus some configurations at the same time
    configurations.removeIf(cfg -> cfg.getSetting() == LICENSE_FILE);

    // Do we have some licence actions ?
    if (!configs.isEmpty()) {

      // take the last one in CLI
      final Configuration configuration = configs.get(configs.size() - 1);
      final Path licenseFile = configuration.getValue().map(Paths::get)
          .orElseThrow(() -> new IllegalArgumentException("Missing value for setting license-file in set command"));

      // validate the path if any
      if (!licenseFile.toFile().exists()) {
        throw new IllegalArgumentException("License file not found: " + licenseFile);
      }

      Collection<Node.Endpoint> peers = findRuntimePeers(node);
      LOGGER.debug("Installing license: {} on nodes: {}", licenseFile, toString(peers));
      upgradeLicense(peers, licenseFile);
      output.info("License installation successful.");
    }
  }
}
