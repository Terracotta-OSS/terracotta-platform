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
import org.terracotta.connection.Connection;
import org.terracotta.exception.ConnectionClosedException;
import org.terracotta.exception.ConnectionShutdownException;
import org.terracotta.management.entity.nms.agent.ReconnectData;
import org.terracotta.management.entity.nms.agent.client.diag.DiagnosticProvider;
import org.terracotta.management.entity.nms.agent.client.diag.DiagnosticUtility;
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * @author Mathieu Carbou
 */
public class DefaultNmsAgentService implements EndpointListener, MessageListener<Message>, NmsAgentService {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultNmsAgentService.class);
  private static final String CAPABILITY_NAME = NmsAgentService.class.getSimpleName();

  private final Supplier<NmsAgentEntity> entitySupplier;
  private final Context root;

  private volatile boolean closed;
  private volatile NmsAgentEntity entity;
  private volatile ManagementRegistry registry;
  private volatile String[] previouslyExposedTags;

  private long timeoutMs = 5000;
  private Executor managementCallExecutor = Runnable::run;
  private final ManagementProvider<?> diagnosticProvider = new DiagnosticProvider(DiagnosticUtility.class);
  private BiConsumer<Operation, Throwable> onOperationError = (op, err) -> LOGGER.trace("Failed to call management entity. Message will be lost. Error: {}", err.getMessage(), err);

  private final ManagementProvider<?> managementProvider = new ManagementProviderAdapter<Object>(CAPABILITY_NAME, Object.class) {
    @Override
    public void register(Object managedObject) {
      refreshManagementRegistry();
    }

    @Override
    public void unregister(Object managedObject) {
      refreshManagementRegistry();
    }
  };

  public DefaultNmsAgentService(Context root, NmsAgentEntity entity) {
    this(root, () -> entity);
  }

  public DefaultNmsAgentService(Context root, Connection connection) {
    this(root, new NmsAgentEntityFactory(connection).retrieve());
  }

  public DefaultNmsAgentService(Context root, Supplier<NmsAgentEntity> entitySupplier) {
    this.root = root;
    this.entitySupplier = entitySupplier;
  }

  // MessageListener

  @Override
  public void onMessage(Message message) {
    LOGGER.trace("onMessage({})", message);
    if (message.getType().equals("MANAGEMENT_CALL")) {
      ContextualCall<?> contextualCall = message.unwrap(ContextualCall.class).get(0);
      getManagementCallExecutor().execute(() -> executeManagementCall(((ManagementCallMessage) message).getManagementCallIdentifier(), contextualCall));
    } else {
      LOGGER.warn("Received unsupported message: " + message);
    }
  }

  // EndpointListener

  @Override
  public Object onReconnect() {
    if (isManagementRegistryBridged()) {
      ManagementRegistry registry = getRegistry();
      Collection<? extends Capability> capabilities = registry == null ? Collections.<Capability>emptyList() : registry.getCapabilities();
      Context context = registry == null ? Context.empty() : root.with(registry.getContextContainer().getName(), registry.getContextContainer().getValue());
      if (registry == null) {
        LOGGER.info("Reconnecting current client with tags: " + Arrays.toString(previouslyExposedTags));
      } else {
        LOGGER.info("Reconnecting current client with existing management registry and tags: " + Arrays.toString(previouslyExposedTags));
      }
      return new ReconnectData(
          previouslyExposedTags,
          root,
          registry == null ? null : registry.getContextContainer(),
          registry == null ? null : capabilities.toArray(new Capability[capabilities.size()]),
          new ContextualNotification(context, "CLIENT_RECONNECTED"));
    } else {
      return null;
    }
  }

  @Override
  public void onDisconnectUnexpectedly() {
    LOGGER.info("Management entity will be flushed following an unexpected disconnection");
    flushEntity();
  }

  // Closeable

  @Override
  public synchronized void close() {
    if (!closed) {
      LOGGER.info("Closing management agent service");
      ManagementRegistry registry = getRegistry();
      // disable bridging
      if (registry != null) {
        registry.removeManagementProvider(managementProvider);
        this.registry = null;
      }
      flushEntity();
      closed = true;
    }
  }

  // NmsAgentService

  @Override
  public boolean isDisconnected() {
    return entity == null;
  }

  @Override
  public boolean isClosed() {
    return closed;
  }

  /**
   * Bridges a management registry with a NMS Agent Entity. All exposure in the registry will be propagated to the server and
   * it will listen for management calls also.
   */
  public void setManagementRegistry(ManagementRegistry registry) {
    LOGGER.trace("setManagementRegistry({})", registry.getContextContainer().getValue());
    if (this.registry == null) {
      // install DiagnosticUtility
      {
        boolean alreadybridged = !registry.getManagementProvidersByCapability("DiagnosticCalls").isEmpty();
        if (!alreadybridged) {
          registry.addManagementProvider(diagnosticProvider);
          registry.register(new DiagnosticUtility());
        }
      }
      // install the bridge
      {
        boolean alreadybridged = registry.getManagementProvidersByCapability(CAPABILITY_NAME)
            .stream()
            .anyMatch(provider -> provider == managementProvider); // need to check by reference to be sure this is the same instance linked to this class
        if (!alreadybridged) {
          registry.addManagementProvider(managementProvider);
        }
      }
      this.registry = registry;
    }
  }

  public ManagementRegistry getRegistry() {
    return registry;
  }

  @Override
  public boolean isManagementRegistryBridged() {
    return registry != null;
  }

  public NmsAgentService setManagementCallExecutor(Executor managementCallExecutor) {
    this.managementCallExecutor = Objects.requireNonNull(managementCallExecutor);
    return this;
  }

  public Executor getManagementCallExecutor() {
    return managementCallExecutor;
  }

  public NmsAgentService setOperationTimeout(long duration, TimeUnit unit) {
    this.timeoutMs = TimeUnit.MILLISECONDS.convert(duration, unit);
    return this;
  }

  public void setOnOperationError(BiConsumer<Operation, Throwable> onOperationError) {
    this.onOperationError = onOperationError;
  }

  // features

  @Override
  public void setCapabilities(ContextContainer contextContainer, Collection<? extends Capability> capabilities) {
    setCapabilities(contextContainer, capabilities.toArray(new Capability[capabilities.size()]));
  }

  @Override
  public void setCapabilities(ContextContainer contextContainer, Capability... capabilities) {
    LOGGER.trace("exposeManagementMetadata({})", contextContainer.getValue());
    runOperation(() -> getEntity().exposeManagementMetadata(null, root, contextContainer, capabilities));
  }

  @Override
  public void setTags(Collection<String> tags) {
    setTags(tags.toArray(new String[tags.size()]));
  }

  @Override
  public void setTags(String... tags) {
    LOGGER.trace("setTags({})", Arrays.asList(tags));
    runOperation(() -> getEntity().exposeTags(null, tags));
    previouslyExposedTags = tags;
  }

  @Override
  public void pushNotification(ContextualNotification notification) {
    if (notification != null) {
      // ensure to send the notification with the root context
      notification.setContext(notification.getContext().with(root));
      LOGGER.trace("pushNotification({})", notification);
      runOperation(() -> getEntity().pushNotification(null, notification));
    }
  }

  @Override
  public void pushStatistics(Collection<ContextualStatistics> statistics) {
    pushStatistics(statistics.toArray(new ContextualStatistics[statistics.size()]));
  }

  @Override
  public void pushStatistics(ContextualStatistics... statistics) {
    if (statistics.length > 0) {
      // ensure to send the stats with the root context
      for (ContextualStatistics statistic : statistics) {
        statistic.setContext(statistic.getContext().with(root));
      }
      LOGGER.trace("pushStatistics({})", statistics.length);
      runOperation(() -> getEntity().pushStatistics(null, statistics));
    }
  }

  @Override
  public void sendStates() {
    LOGGER.info("Sending management registry and tags to server");
    refreshManagementRegistry();
    if (previouslyExposedTags != null) {
      setTags(previouslyExposedTags);
    }
  }

  @Override
  public void flushEntity() {
    NmsAgentEntity entity = this.entity;
    this.entity = null;
    if (entity != null) {
      LOGGER.trace("flushEntity()");
      entity.setEndpointListener(null);
    }
  }

  public void refreshManagementRegistry() {
    // expose the registry each time a new object is registered in the management registry
    if (isManagementRegistryBridged()) {
      ManagementRegistry registry = getRegistry();
      setCapabilities(registry.getContextContainer(), registry.getCapabilities());
    }
  }

  protected void executeManagementCall(String managementCallIdentifier, ContextualCall<?> contextualCall) {
    if (isManagementRegistryBridged()) {
      ContextualReturn<?> aReturn = getRegistry().withCapability(contextualCall.getCapability())
          .call(contextualCall.getMethodName(), contextualCall.getReturnType(), contextualCall.getParameters())
          .on(contextualCall.getContext())
          .build()
          .execute()
          .getSingleResult();
      answerManagementCall(managementCallIdentifier, aReturn);
    }
  }

  protected void answerManagementCall(String managementCallIdentifier, ContextualReturn<?> aReturn) {
    LOGGER.trace("answerManagementCall({}, {})", managementCallIdentifier, aReturn);
    // ensure to send the answer with the root context
    aReturn.setContext(aReturn.getContext().with(root));
    runOperation(() -> getEntity().answerManagementCall(null, managementCallIdentifier, aReturn));
  }

  protected void runOperation(Supplier<Future<?>> op) {
    if (!isClosed()) {
      Future<?> future;
      try {
        future = op.get();
      } catch (ConnectionClosedException | ConnectionShutdownException e) {
        flushEntity();
        onOperationError.accept(() -> runOperation(op), e);
        return;
      }
      try {
        future.get(timeoutMs, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (ExecutionException e) {
        // do not flush entity: these exception do not mean that the connection is broken
        onOperationError.accept(() -> runOperation(op), e.getCause());
      } catch (TimeoutException | RuntimeException e) {
        // do not flush entity: these exception do not mean that the connection is broken
        onOperationError.accept(() -> runOperation(op), e);
      }
    }
  }

  protected NmsAgentEntity getEntity() {
    if (isClosed()) {
      throw new IllegalStateException("closed");
    }

    NmsAgentEntity entity = this.entity;

    // check first if we have one
    if (entity != null) {
      return entity;
    }

    // ask for one
    LOGGER.info("Creating new management agent entity");
    entity = Objects.requireNonNull(entitySupplier.get());
    entity.registerMessageListener(Message.class, this);
    entity.setEndpointListener(this);

    // needed before calling refreshManagementRegistry();
    this.entity = entity;

    // this will call again getEntity();
    refreshManagementRegistry();
    if (previouslyExposedTags != null) {
      setTags(previouslyExposedTags);
    }

    return entity;
  }

  public interface Operation {
    void retry();
  }

}
