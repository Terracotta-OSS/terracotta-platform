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
package org.terracotta.management.entity.management.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.Objects;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.cluster.ClientIdentifier;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementProviderAdapter;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.management.registry.action.ExposedObject;
import org.terracotta.voltron.proxy.MessageListener;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentService implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementAgentService.class);

  private final ManagementAgentEntity entity;
  private ClientIdentifier clientIdentifier;

  private volatile ManagementRegistry registry;
  private volatile boolean bridging = false;
  private Capability[] previouslyExposed = new Capability[0];

  private ContextualReturnListener contextualReturnListener = new ContextualReturnListenerAdapter();
  private long timeout = 5000;
  private Executor managementMessageExecutor = new Executor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };

  private final ManagementProvider<?> managementProvider = new ManagementProviderAdapter<Object>(ManagementAgentService.class.getSimpleName(), Object.class) {
    @Override
    public ExposedObject<Object> register(Object managedObject) {
      refresh(managedObject);
      return null;
    }

    @Override
    public ExposedObject<Object> unregister(Object managedObject) {
      refresh(managedObject);
      return null;
    }

    private void refresh(Object managedObject) {
      // expose the registry each time a new object is registered in the management registry
      if (bridging) {
        try {
          setCapabilities(registry.getContextContainer(), registry.getCapabilities());
        } catch (InterruptedException e) {
          LOGGER.error("Failed to register managed object of type " + managedObject.getClass().getName() + ": " + e.getMessage(), e);
          Thread.currentThread().interrupt();
        } catch (Exception e) {
          LOGGER.error("Failed to register managed object of type " + managedObject.getClass().getName() + ": " + e.getMessage(), e);
        }
      }
    }
  };

  public ManagementAgentService(final ManagementAgentEntity entity) {
    this.entity = Objects.requireNonNull(entity);
    this.entity.registerListener(ManagementCallMessage.class, new MessageListener<ManagementCallMessage>() {
      @Override
      public void onMessage(final ManagementCallMessage message) {
        if (message.getType().equals("MANAGEMENT_CALL")) {
          final ContextualCall contextualCall = message.unwrap(ContextualCall.class).get(0);
          managementMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
              try {
                // check again because in another thread
                if (bridging) {
                  ContextualReturn<?> aReturn = registry.withCapability(contextualCall.getCapability())
                      .call(contextualCall.getMethodName(), contextualCall.getReturnType(), contextualCall.getParameters())
                      .on(contextualCall.getContext())
                      .build()
                      .execute()
                      .getSingleResult();
                  // check again in case the management call takes some time
                  if (bridging) {
                    get(entity.callReturn(null, message.getFrom(), message.getManagementCallIdentifier(), aReturn), timeout);
                  }
                }
              } catch (Exception err) {
                if (LOGGER.isWarnEnabled()) {
                  LOGGER.warn("Error on management call execution or result sending for " + contextualCall + ". Error: " + err.getMessage(), err);
                }
              }
            }
          });

        } else if (message.getType().equals("MANAGEMENT_CALL_RETURN")) {
          final ContextualReturn<?> aReturn = message.unwrap(ContextualReturn.class).get(0);
          managementMessageExecutor.execute(new Runnable() {
            @Override
            public void run() {
              try {
                contextualReturnListener.onContextualReturn(message.getFrom(), message.getManagementCallIdentifier(), aReturn);
              } catch (RuntimeException err) {
                if (LOGGER.isWarnEnabled()) {
                  LOGGER.warn("Error on management call result listener for " + message + ". Error: " + err.getMessage(), err);
                }
              }
            }
          });
        }
      }
    });
  }

  public void init() throws ManagementOperationException, InterruptedException, TimeoutException {
    this.clientIdentifier = get(entity.getClientIdentifier(null), 20000);

    // expose the registry when CM is first available
    if (bridging) {
      Collection<? extends Capability> capabilities = registry.getCapabilities();
      setCapabilities(registry.getContextContainer(), capabilities.toArray(new Capability[capabilities.size()]));
    }
  }

  /**
   * Bridges a management registry with a management entity. All exposure in the registry will be propagated to the server and
   * it will listen for management calls also.
   */
  public synchronized void setManagementRegistry(ManagementRegistry registry) {
    if (!bridging) {
      this.registry = registry;
      registry.addManagementProvider(managementProvider);
      bridging = true;
    }
  }

  @Override
  public synchronized void close() {
    if (bridging) {
      registry.removeManagementProvider(managementProvider);
      bridging = false;
    }
  }

  // config ops

  public ManagementAgentService setContextualReturnListener(ContextualReturnListener contextualReturnListener) {
    this.contextualReturnListener = Objects.requireNonNull(contextualReturnListener);
    return this;
  }

  public ManagementAgentService setManagementMessageExecutor(Executor managementCallExecutor) {
    this.managementMessageExecutor = Objects.requireNonNull(managementCallExecutor);
    return this;
  }

  public ManagementAgentService setOperationTimeout(long duration, TimeUnit unit) {
    this.timeout = TimeUnit.MILLISECONDS.convert(duration, unit);
    return this;
  }

  // features

  public void setCapabilities(ContextContainer contextContainer, Collection<? extends Capability> capabilities) throws ManagementOperationException, InterruptedException, TimeoutException {
    setCapabilities(contextContainer, capabilities.toArray(new Capability[capabilities.size()]));
  }

  public void setCapabilities(ContextContainer contextContainer, Capability... capabilities) throws ManagementOperationException, InterruptedException, TimeoutException {
    if (!Arrays.deepEquals(previouslyExposed, capabilities)) {
      get(entity.exposeManagementMetadata(null, contextContainer, capabilities), timeout);
      previouslyExposed = capabilities;
    }
  }

  public void setTags(Collection<String> tags) throws ManagementOperationException, InterruptedException, TimeoutException {
    setTags(tags.toArray(new String[tags.size()]));
  }

  public void setTags(String... tags) throws ManagementOperationException, InterruptedException, TimeoutException {
    get(entity.exposeTags(null, tags), timeout);
  }

  public void pushNotification(ContextualNotification notification) throws ManagementOperationException, InterruptedException, TimeoutException {
    if (notification != null) {
      notification.setContext(notification.getContext().with("clientId", getClientIdentifier().getClientId()));
      get(entity.pushNotification(null, notification), timeout);
    }
  }

  public void pushStatistics(ContextualStatistics... statistics) throws ManagementOperationException, InterruptedException, TimeoutException {
    if (statistics.length > 0) {
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with("clientId", getClientIdentifier().getClientId()));
      }
      get(entity.pushStatistics(null, statistics), timeout);
    }
  }

  public void pushStatistics(Collection<ContextualStatistics> statistics) throws ManagementOperationException, InterruptedException, TimeoutException {
    pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()]));
  }

  public ClientIdentifier getClientIdentifier() {
    return Objects.requireNonNull(clientIdentifier);
  }

  /**
   * Execute a management call and do not expect any return result.
   * <p>
   * Returns a unique identifier for this management call.
   */
  public String call(ClientIdentifier to, Context context, String capabilityName, String methodName, Class<?> returnType, Parameter... parameters) throws ManagementOperationException, InterruptedException, TimeoutException {
    return get(entity.call(null, to, context, capabilityName, methodName, returnType, parameters), timeout);
  }

  private static <V> V get(Future<V> future, long timeout) throws ManagementOperationException, TimeoutException, InterruptedException {
    try {
      return future.get(timeout, TimeUnit.MILLISECONDS);
    } catch (ExecutionException e) {
      throw new ManagementOperationException(e.getCause());
    }
  }

}
