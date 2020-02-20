/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.nio.file.Path;

/**
 * @author Mathieu Carbou
 */
public interface TcConfigMapper {
  /**
   * Reads list of tc configuration xml file and output the Cluster object.
   */
  Cluster parseConfig(String clusterName, Path... tcConfigPaths);
}
