/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.offheapresource;

import java.io.IOException;
import java.util.Collection;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceProvider;
import org.terracotta.entity.ServiceProviderCleanupException;
import org.terracotta.entity.ServiceProviderConfiguration;

/**
 *
 * @author cdennis
 */
class OffHeapResourcesProvider implements ServiceProvider {

  @Override
  public boolean initialize(ServiceProviderConfiguration configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public <T> T getService(long consumerID, ServiceConfiguration<T> configuration) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Class<?>> getProvidedServiceTypes() {
    throw new UnsupportedOperationException();
  }

  @Override
  public void close() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void clear() throws ServiceProviderCleanupException {
    throw new UnsupportedOperationException();
  }
}
