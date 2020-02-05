/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

public interface ConfigStorage<T> {
  T getConfig(long version) throws ConfigStorageException;

  void saveConfig(long version, T config) throws ConfigStorageException;

  /**
   * Clear the saved configs
   */
  void reset() throws ConfigStorageException;
}
