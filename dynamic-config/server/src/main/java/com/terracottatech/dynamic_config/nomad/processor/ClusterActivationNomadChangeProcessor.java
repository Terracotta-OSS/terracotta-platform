/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.processor;

import com.terracottatech.dynamic_config.nomad.ClusterActivationNomadChange;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.xml.XmlConfiguration;
import com.terracottatech.nomad.server.NomadException;

import static java.util.Objects.requireNonNull;

public class ClusterActivationNomadChangeProcessor implements NomadChangeProcessor<ClusterActivationNomadChange> {

  private final ConfigController configController;

  public ClusterActivationNomadChangeProcessor(ConfigController configController) {
    this.configController = requireNonNull(configController);
  }

  @Override
  public String getConfigWithChange(String baseConfig, ClusterActivationNomadChange change) throws NomadException {
    if (baseConfig != null) {
      throw new NomadException("baseConfig must be null");
    }

    try {
      return new XmlConfiguration(
          change.getCluster(),
          this.configController.getStripeName(),
          this.configController.getNodeName()
      ).toString();
    } catch (Exception e) {
      throw new NomadException("Caught exception while converting cluster config to xml", e);
    }
  }

  @Override
  public void applyChange(ClusterActivationNomadChange change) {
    // no-op
  }
}
