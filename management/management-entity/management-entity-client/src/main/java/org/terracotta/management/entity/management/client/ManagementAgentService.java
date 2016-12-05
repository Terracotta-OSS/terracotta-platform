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
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;
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

  private volatile ManagementRegistry registry;
  private volatile boolean bridging = false;
  private Capability[] previouslyExposed = new Capability[0];

  private long timeout = 5000;
  private Executor managementCallExecutor = new Executor() {
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
    this.entity.registerListener(Message.class, new MessageListener<Message>() {
      @Override
      public void onMessage(final Message message) {
        LOGGER.trace("onMessage({})", message);

        if (message.getType().equals("MANAGEMENT_CALL")) {
          final ContextualCall<?> contextualCall = message.unwrap(ContextualCall.class).get(0);
          managementCallExecutor.execute(new Runnable() {
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
                    LOGGER.trace("answerManagementCall({}, {})", message, contextualCall);
                    get(entity.answerManagementCall(null, ((ManagementCallMessage) message).getManagementCallIdentifier(), aReturn));
                  }
                }
              } catch (Exception err) {
                if (LOGGER.isWarnEnabled()) {
                  LOGGER.warn("Error on management call execution or result sending for " + contextualCall + ". Error: " + err.getMessage(), err);
                }
              }
            }
          });

        } else {
          LOGGER.warn("Received unsupported message: " + message);
        }
      }
    });
  }

  public void init() throws ExecutionException, InterruptedException, TimeoutException {
    LOGGER.trace("init()");
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

  public ManagementAgentService setManagementCallExecutor(Executor managementCallExecutor) {
    this.managementCallExecutor = Objects.requireNonNull(managementCallExecutor);
    return this;
  }

  public ManagementAgentService setOperationTimeout(long duration, TimeUnit unit) {
    this.timeout = TimeUnit.MILLISECONDS.convert(duration, unit);
    return this;
  }

  // features

  public void setCapabilities(ContextContainer contextContainer, Collection<? extends Capability> capabilities) throws ExecutionException, InterruptedException, TimeoutException {
    setCapabilities(contextContainer, capabilities.toArray(new Capability[capabilities.size()]));
  }

  public void setCapabilities(ContextContainer contextContainer, Capability... capabilities) throws ExecutionException, InterruptedException, TimeoutException {
    if (!Arrays.deepEquals(previouslyExposed, capabilities)) {
      LOGGER.trace("exposeManagementMetadata({})", contextContainer);
      get(entity.exposeManagementMetadata(null, contextContainer, capabilities));
      previouslyExposed = capabilities;
    }
  }

  public void setTags(Collection<String> tags) throws ExecutionException, InterruptedException, TimeoutException {
    setTags(tags.toArray(new String[tags.size()]));
  }

  public void setTags(String... tags) throws ExecutionException, InterruptedException, TimeoutException {
    LOGGER.trace("setTags({})", Arrays.asList(tags));
    get(entity.exposeTags(null, tags));
  }

  public void pushNotification(ContextualNotification notification) throws ExecutionException, InterruptedException, TimeoutException {
    if (notification != null) {
      LOGGER.trace("pushNotification({})", notification);
      get(entity.pushNotification(null, notification));
    }
  }

  public void pushStatistics(ContextualStatistics... statistics) throws ExecutionException, InterruptedException, TimeoutException {
    if (statistics.length > 0) {
      LOGGER.trace("pushStatistics({})", statistics.length);
      get(entity.pushStatistics(null, statistics));
    }
  }

  public void pushStatistics(Collection<ContextualStatistics> statistics) throws ExecutionException, InterruptedException, TimeoutException {
    pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()]));
  }

  private <V> V get(Future<V> future) throws ExecutionException, TimeoutException, InterruptedException {
    return future.get(timeout, TimeUnit.MILLISECONDS);
  }

}
