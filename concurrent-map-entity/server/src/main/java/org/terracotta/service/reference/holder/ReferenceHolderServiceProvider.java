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
package org.terracotta.service.reference.holder;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

import com.tc.classloader.BuiltinService;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@BuiltinService
public class ReferenceHolderServiceProvider implements ServiceProvider {

  private final ConcurrentMap<Long, ReferenceHolderService> mappings = new ConcurrentHashMap<Long, ReferenceHolderService>();

  @Override
  public boolean initialize(final ServiceProviderConfiguration serviceProviderConfiguration) {
    return true;
  }

  @Override
  public <T> T getService(final long consumerID, final ServiceConfiguration<T> serviceConfiguration) {
    if(ReferenceHolderService.class.isAssignableFrom(serviceConfiguration.getServiceType())) {
      mappings.putIfAbsent(consumerID, new TransientReferenceHolderService());
      return (T) mappings.get(consumerID);
    } else {
      throw new IllegalArgumentException("Unexpected configuration type " + serviceConfiguration.getClass());
    }
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    return (Collection) Collections.singleton(ReferenceHolderService.class);
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    mappings.clear();
  }
}
