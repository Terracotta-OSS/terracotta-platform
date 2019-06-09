/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.exception.NomadConfigurationException;

import java.io.Closeable;
import java.nio.file.Path;


public interface NomadServerManager extends Closeable {

  /**
   * Initializes the Nomad
   * @param nomadRoot Configuration repository root
   * @throws NomadConfigurationException if initialization of underlying server fails.
   */
  void init(Path nomadRoot) throws NomadConfigurationException;

  /**
   * Makes Nomad server capable of write operations.
   * @param nodeName   Name of the running node
   * @param stripeName Name of the stripe where the node belongs
   */
  void upgradeForWrite(String nodeName, String stripeName);

  /**
   * Used for getting the configuration string from Nomad
   * @return Stored configuration as a String
   * @throws NomadConfigurationException if this method is invoked before {@link #init(Path) method} or if configuration
   * is unavailable or corrupted
   */
  String getConfiguration() throws NomadConfigurationException;

  /**
   * Used for overriding corrupted repository content
   * @param configuration Configuration string which will override the existing configuration (if any)
   * @param version       Version of the configuration. This can be used to keep the configuration version in sync with other
   *                      servers.
   * @throws NomadConfigurationException if underlying server fails to override corrupted repository content
   */
  void repairConfiguration(String configuration, long version) throws NomadConfigurationException;

  /**
   * Used to perform cleanup
   */
  @Override
  void close();
}
