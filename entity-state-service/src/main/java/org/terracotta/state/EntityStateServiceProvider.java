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

package org.terracotta.state;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;
import org.terracotta.state.config.TransientEntityStateRepositoryConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * {@link ServiceProvider} for {@link EntityStateRepository}
 */
public class EntityStateServiceProvider implements ServiceProvider {

  private ConcurrentMap<Long, EntityStateRepository> stateRepositoryMap = new ConcurrentHashMap<Long, EntityStateRepository>();


  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    return true;
  }

  //TODO: check generics once again
  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    EntityStateRepository stateRepository;
    if (configuration instanceof TransientEntityStateRepositoryConfig) {
      if ((stateRepository = stateRepositoryMap.get(consumerID)) == null) {
        stateRepository = new TransientEntityStateRepository();
        stateRepositoryMap.put(consumerID, stateRepository);
      }
      return (T) stateRepository;
    }
    throw new IllegalArgumentException("Unexpected configuration type " + configuration.getClass());
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    List<Class<?>> classes = new ArrayList<Class<?>>();
    classes.add(TransientEntityStateRepository.class);
    return classes;
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    stateRepositoryMap.clear();
  }
}
