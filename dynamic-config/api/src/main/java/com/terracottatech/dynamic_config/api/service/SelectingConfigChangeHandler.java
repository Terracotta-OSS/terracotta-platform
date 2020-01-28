/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.api.service;

import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Configuration;
import com.terracottatech.dynamic_config.api.model.NodeContext;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * ConfigChangeHandler that will select a ConfigChangeHandler from its list depending on some keys extracted from the change
 *
 * @author Mathieu Carbou
 */
public class SelectingConfigChangeHandler<T> implements ConfigChangeHandler {

  private final Map<T, ConfigChangeHandler> handlers = new ConcurrentHashMap<>();

  // the selector to generate a key. By default, does nothing and forward to the fallback.
  private Function<Configuration, T> selector = change -> null;

  // by default we reject the change if no handler is found, or the key is null
  private ConfigChangeHandler fallback = ConfigChangeHandler.reject();

  /**
   * Add a config handler for a key
   */
  public SelectingConfigChangeHandler<T> add(T key, ConfigChangeHandler handler) {
    handlers.put(requireNonNull(key), requireNonNull(handler));
    return this;
  }

  /**
   * Define a fallback in case no handler is found
   */
  public SelectingConfigChangeHandler<T> fallback(ConfigChangeHandler handler) {
    this.fallback = requireNonNull(handler);
    return this;
  }

  /**
   * Define a fallback in case no handler is found
   */
  public SelectingConfigChangeHandler<T> selector(Function<Configuration, T> selector) {
    this.selector = requireNonNull(selector);
    return this;
  }

  @Override
  public Cluster tryApply(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    T key = selector.apply(change);
    if (key != null) {
      ConfigChangeHandler handler = handlers.get(key);
      if (handler != null) {
        return handler.tryApply(baseConfig, change);
      }
    }
    return fallback.tryApply(baseConfig, change);
  }

  @Override
  public boolean apply(Configuration change) {
    T key = selector.apply(change);
    if (key != null) {
      ConfigChangeHandler handler = handlers.get(key);
      if (handler != null) {
        return handler.apply(change);
      } else {
        return fallback.apply(change);
      }
    } else {
      return fallback.apply(change);
    }
  }
}
