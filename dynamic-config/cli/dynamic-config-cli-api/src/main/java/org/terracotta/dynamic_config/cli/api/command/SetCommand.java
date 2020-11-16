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

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Operation;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static java.util.Optional.empty;
import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;

public class SetCommand extends ConfigurationMutationCommand {

  public SetCommand() {
    super(Operation.SET);
  }

  @Override
  public void validate() {
    super.validate();

    // we support a list in case the user inputs: set -c license-file=foo/one.xml -c license-file=foo/two.xml
    // this is not illegal, would work, but a little stupid.
    // But could be useful in case the CLI is scripted and duplications happens (latter command overwrite previous ones)
    Path licenseFile = configurations.stream()
        .filter(configuration -> configuration.getSetting() == LICENSE_FILE)
        .map(Configuration::getValue)
        .findAny()
        .orElse(empty())
        .map(Paths::get)
        .orElse(null);

    if (licenseFile != null) {
      if (!licenseFile.toFile().exists()) {
        throw new IllegalArgumentException("License file not found: " + licenseFile);
      }

      // we remove the license parameters from the list of inputted commands
      // this will allow to being able to update the license plus some configurations at the same time
      configurations.removeIf(configuration -> configuration.getSetting() == LICENSE_FILE);

      Collection<Node.Endpoint> peers = findRuntimePeers(node);
      logger.debug("Importing license: {} on nodes: {}", licenseFile, toString(peers));
      upgradeLicense(peers, licenseFile);
    }
  }
}
