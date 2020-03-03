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

  @Override
  public void reset() throws ConfigStorageException {
    underlying.reset();
  }
}
