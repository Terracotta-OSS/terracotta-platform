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
package org.terracotta.dynamic_config.api.service;

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;

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
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    T key = selector.apply(change);
    if (key != null) {
      ConfigChangeHandler handler = handlers.get(key);
      if (handler != null) {
        handler.validate(baseConfig, change);
      } else {
        fallback.validate(baseConfig, change);
      }
    } else {
      fallback.validate(baseConfig, change);
    }
  }

  @Override
  public void apply(Configuration change) {
    T key = selector.apply(change);
    if (key != null) {
      ConfigChangeHandler handler = handlers.get(key);
      if (handler != null) {
        handler.apply(change);
      } else {
        fallback.apply(change);
      }
    } else {
      fallback.apply(change);
    }
  }
}
