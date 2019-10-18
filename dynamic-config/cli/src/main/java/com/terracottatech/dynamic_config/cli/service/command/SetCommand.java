/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Operation;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;

import static com.terracottatech.dynamic_config.model.Setting.LICENSE_FILE;

@Parameters(commandNames = "set", commandDescription = "Set properties in the cluster or in a node")
@Usage("set -s HOST -c NAMESPACE1.PROPERTY1=VALUE1 [-c NAMESPACE2.PROPERTY2=VALUE2]...")
public class SetCommand extends ConfigurationMutationCommand {
  private boolean licenseUpdate;

  public SetCommand() {
    super(Operation.SET);
  }

  @Override
  public void validate() {
    super.validate();
    licenseUpdate = configurations.stream().anyMatch(configuration -> configuration.getSetting() == LICENSE_FILE);

    if (configurations.size() > 1 && licenseUpdate) {
      throw new ParameterException("Updating the license cannot be combined with any other changes");
    }
  }

  @Override
  public void run() {
    if (licenseUpdate) {
      Collection<InetSocketAddress> peers = findRuntimePeers(node);
      logger.info("Updating license on nodes: {}", toString(peers));
      Path licenseFile = Paths.get(configurations.get(0).getValue());
      upgradeLicense(peers, licenseFile);
    } else {
      super.run();
    }
  }
}
