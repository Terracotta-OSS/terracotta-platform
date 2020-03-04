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
package org.terracotta.management.entity.nms.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public class DefaultNmsService implements NmsService, Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(NmsService.class);

  private final NmsEntity entity;
  private final BlockingQueue<Optional<Message>> incomingMessageQueue;
  private final ConcurrentMap<String, CompletableFuture<ContextualReturn<?>>> managementCallAnswers = new ConcurrentHashMap<>();

  private long timeout = 5000;

  public DefaultNmsService(final NmsEntity entity) {
    this(entity, new LinkedBlockingQueue<>());
  }

  public DefaultNmsService(final NmsEntity entity, BlockingQueue<Optional<Message>> incomingMessageQueue) {
    this(entity, incomingMessageQueue, message -> LOGGER.warn("Queue is full - Message lost: ", message));
  }

  public DefaultNmsService(final NmsEntity entity, BlockingQueue<Optional<Message>> incomingMessageQueue, Consumer<Message> sink) {
    Objects.requireNonNull(sink);
    this.entity = Objects.requireNonNull(entity);
    this.incomingMessageQueue = Objects.requireNonNull(incomingMessageQueue);
    this.entity.registerMessageListener(Message.class, message -> {
      LOGGER.trace("onMessage({})", message);

      switch (message.getType()) {

        case "MANAGEMENT_CALL_RETURN":
          String managementCallIdentifier = ((ManagementCallMessage) message).getManagementCallIdentifier();
          ContextualReturn<?> contextualReturn = message.unwrap(ContextualReturn.class).get(0);
          getManagementAnswerFor(managementCallIdentifier).complete(contextualReturn);
          break;

        case "NOTIFICATION":
        case "STATISTICS":
          boolean offered = incomingMessageQueue.offer(Optional.of(message));
          if (!offered) {
            sink.accept(message);
          }
          break;

        default:
          LOGGER.warn("Received unsupported message: " + message);

      }
    });
  }

  public NmsEntity getEntity() {
    return entity;
  }

  @Override
  public void close() {
    cancelAllManagementCalls();
    // This close call is important.
    // We have to close as much as possible the nms entities we have fetched.
    // If we do not, then, when connection closes, the server can keep some "phantom" fetches
    // on the server entity and thus it will prevent the entity from being destroyed.
    // This is a server bug.
    // This line does not solve completely the issue but limits its probability to happen.
    // But at any point, when you know that the connection is open, you'd better close any entity
    // endpoint. Otherwise, if you do not know the state of the connection, be careful because this
    // method can block.
    entity.close();
  }

  @Override
  public NmsService setOperationTimeout(long duration, TimeUnit unit) {
    this.timeout = TimeUnit.MILLISECONDS.convert(duration, unit);
    return this;
  }

  @Override
  public Cluster readTopology() throws TimeoutException, InterruptedException, ExecutionException {
    return get(entity.readTopology());
  }

  @Override
  public Message waitForMessage() throws InterruptedException {
    Optional<Message> o = incomingMessageQueue.take();
    if (!o.isPresent()) {
      throw new InterruptedException();
    }
    return o.get();
  }

  @Override
  public Message waitForMessage(long time, TimeUnit unit) throws InterruptedException, TimeoutException {
    Optional<Message> o = incomingMessageQueue.poll(time, unit);
    if (o == null) {
      throw new TimeoutException("No message arrived within " + time + " " + unit);
    }
    if (!o.isPresent()) {
      throw new InterruptedException();
    }
    return o.get();
  }

  @Override
  public List<Message> readMessages() {
    List<Optional<Message>> optionals = new ArrayList<>(incomingMessageQueue.size());
    incomingMessageQueue.drainTo(optionals);
    List<Message> messages = optionals.stream().filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    if (!messages.isEmpty()) {
      messages.sort(MESSAGE_COMPARATOR);
    }
    return messages;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> ManagementCall<T> call(Context context, String capabilityName, String methodName, Class<T> returnType, Parameter... parameters) throws InterruptedException, ExecutionException, TimeoutException {
    LOGGER.trace("call({}, {}, {})", context, capabilityName, methodName);

    // trigger the management call on the server
    String managementCallId = get(entity.call(null, context, capabilityName, methodName, returnType, parameters));

    // create a future response for client
    VoltronManagementCall<T> managementCall = new VoltronManagementCall<>(managementCallId, context, returnType, timeout, that -> managementCallAnswers.remove(managementCallId));

    // add handler to complete the call when response will be (or already is) received
    getManagementAnswerFor(managementCallId).whenComplete((aReturn, stopper) -> {
      if (stopper != null) {
        // in case we interrupt the management calls with the #cancelAllManagementCalls() method 
        managementCall.completeExceptionally(stopper);
      } else {
        try {
          // we have a value returned
          managementCall.complete((T) aReturn.getValue());
        } catch (ExecutionException e) {
          // an exception occurred while calling the target method
          managementCall.completeExceptionally(e.getCause());
        } catch (NoSuchElementException e) {
          // no target found for this management call
          managementCall.completeExceptionally(new IllegalManagementCallException(aReturn.getContext(), aReturn.getCapability(), aReturn.getMethodName()));
        }
      }
    });

    return managementCall;
  }

  @Override
  public void cancelAllManagementCalls() {
    InterruptedException stopper = new InterruptedException();
    while (!managementCallAnswers.isEmpty()) {
      managementCallAnswers.keySet().forEach(k -> {
        CompletableFuture<ContextualReturn<?>> removed = managementCallAnswers.remove(k);
        if (removed != null) {
          removed.completeExceptionally(stopper);
        }
      });
    }
  }

  private <V> V get(Future<V> future) throws ExecutionException, TimeoutException, InterruptedException {
    return future.get(timeout, TimeUnit.MILLISECONDS);
  }

  private CompletableFuture<ContextualReturn<?>> getManagementAnswerFor(String managementCallIdentifier) {
    return managementCallAnswers.computeIfAbsent(managementCallIdentifier, s -> new CompletableFuture<>());
  }

}
