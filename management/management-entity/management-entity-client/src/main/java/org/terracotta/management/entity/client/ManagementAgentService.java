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
package org.terracotta.management.entity.client;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementProviderAdapter;
import org.terracotta.management.registry.ManagementRegistry;

import java.io.Closeable;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentService implements Closeable {

  private final ManagementRegistry registry;
  private final ManagementAgentEntity entity;

  private ClientIdentifier clientIdentifier;
  private long timeout = 5000;
  private boolean closed = true;

  private final ManagementProvider<?> managementProvider = new ManagementProviderAdapter<Object>(ManagementAgentService.class.getSimpleName(), Object.class) {
    @Override
    public void register(Object managedObject) {
      // expose the registry each time a new object is registered in the management registry
      if (!closed) {
        setCapabilities(registry.getContextContainer(), registry.getCapabilities());
      }
    }

    @Override
    public void unregister(Object managedObject) {
      // expose the registry each time a new object is registered in the management registry
      if (!closed) {
        setCapabilities(registry.getContextContainer(), registry.getCapabilities());
      }
    }
  };

  public ManagementAgentService(ManagementRegistry registry, ManagementAgentEntity entity) {
    this.registry = Objects.requireNonNull(registry);
    this.entity = Objects.requireNonNull(entity);
  }

  public synchronized void init() {
    if (closed) {

      clientIdentifier = get(entity.getClientIdentifier(null), 20000);

      registry.addManagementProvider(managementProvider);

      // expose the registry when CM is first available
      Collection<Capability> capabilities = registry.getCapabilities();
      get(entity.exposeManagementMetadata(null, registry.getContextContainer(), capabilities.toArray(new Capability[capabilities.size()])), timeout);

      closed = false;
    }
  }

  @Override
  public synchronized void close() {
    if (!closed) {
      registry.removeManagementProvider(managementProvider);
      closed = true;
    }
  }

  public void setOperationTimeout(long duration, TimeUnit unit) {
    this.timeout = TimeUnit.MILLISECONDS.convert(duration, unit);
  }

  public void setCapabilities(ContextContainer contextContainer, Collection<Capability> capabilities) {
    setCapabilities(contextContainer, capabilities.toArray(new Capability[capabilities.size()]));
  }

  public void setCapabilities(ContextContainer contextContainer, Capability... capabilities) {
    if (!closed) {
      get(entity.exposeManagementMetadata(null, contextContainer, capabilities), timeout);
    }
  }

  public void setTags(Collection<String> tags) {
    setTags(tags.toArray(new String[tags.size()]));
  }

  public void setTags(String... tags) {
    if (!closed) {
      get(entity.exposeTags(null, tags), timeout);
    }
  }

  public void pushNotification(ContextualNotification notification) {
    if (!closed && notification != null) {
      notification.setContext(notification.getContext().with("clientId", clientIdentifier.getClientId()));
      get(entity.pushNotification(null, notification), timeout);
    }
  }

  public void pushStatistics(ContextualStatistics... statistics) {
    if (!closed && statistics.length > 0) {
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with("clientId", clientIdentifier.getClientId()));
      }
      get(entity.pushStatistics(null, statistics), timeout);
    }
  }

  public void pushStatistics(Collection<ContextualStatistics> statistics) {
    pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()]));
  }

  public ClientIdentifier getClientIdentifier() {
    return clientIdentifier;
  }

  private static <V> V get(Future<V> future, long timeout) {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    } catch (ExecutionException e) {
      throw new IllegalStateException(e.getCause());
    } catch (TimeoutException e) {
      throw new IllegalStateException("Timed out after " + timeout + "ms.", e);
    }
  }

}
