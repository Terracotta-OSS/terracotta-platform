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
package org.terracotta.management.entity.nms.agent.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.management.entity.nms.agent.ReconnectData;
import org.terracotta.management.entity.nms.agent.client.diag.DiagnosticProvider;
import org.terracotta.management.entity.nms.agent.client.diag.DiagnosticUtility;
import org.terracotta.management.model.Objects;
import org.terracotta.management.model.call.ContextualCall;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.capabilities.Capability;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.model.notification.ContextualNotification;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.ManagementProvider;
import org.terracotta.management.registry.ManagementProviderAdapter;
import org.terracotta.management.registry.ManagementRegistry;
import org.terracotta.voltron.proxy.MessageListener;
import org.terracotta.voltron.proxy.client.EndpointListener;

import java.io.Closeable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Mathieu Carbou
 */
public class NmsAgentService implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NmsAgentService.class);

  private final NmsAgentEntity entity;

  private volatile ManagementRegistry registry;
  private volatile boolean bridging = false;
  private volatile boolean disconnected = false;
  private Capability[] previouslyExposedCapabilities;
  private String[] previouslyExposedTags;

  private long timeoutMs = 5000;
  private Executor managementCallExecutor = new Executor() {
    @Override
    public void execute(Runnable command) {
      command.run();
    }
  };

  private final ManagementProvider<?> managementProvider = new ManagementProviderAdapter<Object>(NmsAgentService.class.getSimpleName(), Object.class) {
    @Override
    public void register(Object managedObject) {
      refresh(managedObject);
    }

    @Override
    public void unregister(Object managedObject) {
      refresh(managedObject);
    }

    private void refresh(Object managedObject) {
      // expose the registry each time a new object is registered in the management registry
      if (bridging) {
        try {
          setCapabilities(registry.getContextContainer(), registry.getCapabilities());
        } catch (InterruptedException e) {
          LOGGER.error("Failed to register managed object of type " + managedObject.getClass().getName() + ": " + e.getMessage(), e);
          Thread.currentThread().interrupt();
        } catch (ConnectionClosedException e) {
          NmsAgentService.this.close();
        } catch (Exception e) {
          LOGGER.error("Failed to register managed object of type " + managedObject.getClass().getName() + ": " + e.getMessage(), e);
        }
      }
    }
  };
  private ManagementProvider<?> diagnosticProvider = new DiagnosticProvider(DiagnosticUtility.class);

  public NmsAgentService(final NmsAgentEntity entity) {
    this.entity = Objects.requireNonNull(entity);

    this.entity.registerMessageListener(Message.class, new MessageListener<Message>() {
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
                    try {
                      get(entity.answerManagementCall(null, ((ManagementCallMessage) message).getManagementCallIdentifier(), aReturn));
                    } catch (ConnectionClosedException e) {
                      disconnected = true;
                      throw e;
                    }
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

    this.entity.setEndpointListener(new EndpointListener() {
      @Override
      public Object onReconnect() {
        if (bridging) {
          LOGGER.trace("onReconnect()");
          Collection<? extends Capability> capabilities = registry == null ? Collections.<Capability>emptyList() : registry.getCapabilities();
          Context context = registry == null ? Context.empty() : Context.create(registry.getContextContainer().getName(), registry.getContextContainer().getValue());
          return new ReconnectData(
              previouslyExposedTags,
              registry == null ? null : registry.getContextContainer(),
              registry == null ? null : capabilities.toArray(new Capability[capabilities.size()]),
              new ContextualNotification(context, "CLIENT_RECONNECTED"));
        } else {
          return null;
        }
      }

      @Override
      public void onDisconnectUnexpectedly() {
        LOGGER.trace("onDisconnectUnexpectedly()");
        close();
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
   * Bridges a management registry with a NMS Agent Entity. All exposure in the registry will be propagated to the server and
   * it will listen for management calls also.
   */
  public synchronized void setManagementRegistry(ManagementRegistry registry) {
    if (!bridging) {
      LOGGER.trace("setManagementRegistry({})", registry.getContextContainer().getValue());
      this.registry = registry;
      registry.addManagementProvider(diagnosticProvider);
      // management provider must be added last
      registry.addManagementProvider(managementProvider);
      registry.register(new DiagnosticUtility());
      bridging = true;
    }
  }

  public boolean isDisconnected() {
    return disconnected;
  }

  @Override
  public synchronized void close() {
    if (bridging) {
      LOGGER.trace("close()");
      registry.removeManagementProvider(managementProvider);
      bridging = false;
    }
  }

  // config ops

  public NmsAgentService setManagementCallExecutor(Executor managementCallExecutor) {
    this.managementCallExecutor = Objects.requireNonNull(managementCallExecutor);
    return this;
  }

  public NmsAgentService setOperationTimeout(long duration, TimeUnit unit) {
    this.timeoutMs = TimeUnit.MILLISECONDS.convert(duration, unit);
    return this;
  }

  // features

  public void setCapabilities(ContextContainer contextContainer, Collection<? extends Capability> capabilities) throws ExecutionException, InterruptedException, TimeoutException {
    setCapabilities(contextContainer, capabilities.toArray(new Capability[capabilities.size()]));
  }

  public void setCapabilities(ContextContainer contextContainer, Capability... capabilities) throws ExecutionException, InterruptedException, TimeoutException {
    if (!Arrays.deepEquals(previouslyExposedCapabilities, capabilities)) {
      LOGGER.trace("exposeManagementMetadata({})", contextContainer.getValue());
      get(entity.exposeManagementMetadata(null, contextContainer, capabilities));
      previouslyExposedCapabilities = capabilities;
    }
  }

  public void setTags(Collection<String> tags) throws ExecutionException, InterruptedException, TimeoutException {
    setTags(tags.toArray(new String[tags.size()]));
  }

  public void setTags(String... tags) throws ExecutionException, InterruptedException, TimeoutException {
    LOGGER.trace("setTags({})", Arrays.asList(tags));
    get(entity.exposeTags(null, tags));
    previouslyExposedTags = tags;
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
    return future.get(timeoutMs, TimeUnit.MILLISECONDS);
  }

}
