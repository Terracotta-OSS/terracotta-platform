/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

public class InitialConfigStorage<T> implements ConfigStorage<T> {
  private static final long INITIAL_VERSION = 0L;

  private final ConfigStorage<T> underlying;

  public InitialConfigStorage(ConfigStorage<T> underlying) {
    this.underlying = underlying;
  }

  @Override
  public T getConfig(long version) throws ConfigStorageException {
    if (version == INITIAL_VERSION) {
      return null;
    }

    return underlying.getConfig(version);
  }

  @Override
  public void saveConfig(long version, T config) throws ConfigStorageException {
    if (version == INITIAL_VERSION) {
      throw new AssertionError("Invalid version: " + version);
    }

    underlying.saveConfig(version, config);
  }
}
