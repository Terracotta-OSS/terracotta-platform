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
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Operation;
import org.terracotta.dynamic_config.cli.command.Usage;

import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static org.terracotta.dynamic_config.api.model.Setting.LICENSE_FILE;

@Parameters(commandNames = "set", commandDescription = "Set configuration properties")
@Usage("set -s <hostname[:port]> -c <[namespace:]property=value>,<[namespace:]property=value>...")
public class SetCommand extends ConfigurationMutationCommand {

  private Path licenseFile;

  public SetCommand() {
    super(Operation.SET);
  }

  @Override
  public void validate() {
    super.validate();

    // we support a list in case the user inputs: set -c license-file=foo/one.xml -c license-file=foo/two.xml
    // this is not illegal, would work, but a little stupid.
    // But could be useful in case the CLI is scripted and duplications happens (latter command overwrite previous ones)
    licenseFile = configurations.stream()
        .filter(configuration -> configuration.getSetting() == LICENSE_FILE)
        .map(Configuration::getValue)
        .map(Paths::get)
        .findAny()
        .orElse(null);


    if (licenseFile != null) {
      if (!Files.exists(licenseFile)) {
        throw new ParameterException("License file not found: " + licenseFile);
      }

      // we remove the license parameters from the list of inputted commands
      // this will allow to being able to update the license plus some configurations at the same time
      configurations.removeIf(configuration -> configuration.getSetting() == LICENSE_FILE);
    }
  }

  @Override
  public void run() {
    if (licenseFile != null) {
      Collection<InetSocketAddress> peers = findRuntimePeers(node);
      logger.info("Importing license: {} on nodes: {}", licenseFile, toString(peers));
      upgradeLicense(peers, licenseFile);
    }
    // then let the super class run to apply eventual other settings in the CLI
    super.run();
  }
}
