/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.server.nomad.persistence;

/**
 * @author Mathieu Carbou
 */
public class ConfigStorageAdapter<T> implements ConfigStorage<T> {

  private final ConfigStorage<T> delegate;

  public ConfigStorageAdapter(ConfigStorage<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T getConfig(long version) throws ConfigStorageException {return delegate.getConfig(version);}

  @Override
  public void saveConfig(long version, T config) throws ConfigStorageException {delegate.saveConfig(version, config);}
}
