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
import org.terracotta.management.model.Objects;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.ManagementCallMessage;
import org.terracotta.management.model.message.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * @author Mathieu Carbou
 */
public class DefaultNmsService implements NmsService {

  private static final Logger LOGGER = LoggerFactory.getLogger(NmsService.class);

  private final Queue<VoltronManagementCall<?>> managementCalls = new ConcurrentLinkedQueue<>();
  private final NmsEntity entity;
  private final BlockingQueue<Optional<Message>> incomingMessageQueue;

  // this RW lock is to prevent any message listener callback to iterate over the list of managementCalls
  // before this list gets updated in the call() method.
  // I.e. it could be possible that the entity.call() is executed fast and the callback to get the
  // MANAGEMENT_CALL_RETURN message is called before the VoltronManagementCall object is put in the
  // managementCalls list
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private long timeout = 5000;
  
  public DefaultNmsService(final NmsEntity entity) {
    this(entity, new LinkedBlockingQueue<>());
  }

  public DefaultNmsService(final NmsEntity entity, BlockingQueue<Optional<Message>> incomingMessageQueue) {
    this.entity = Objects.requireNonNull(entity);
    this.incomingMessageQueue = incomingMessageQueue;
    this.entity.registerMessageListener(Message.class, message -> {
      LOGGER.trace("onMessage({})", message);

      switch (message.getType()) {

        case "MANAGEMENT_CALL_RETURN":
          lock.readLock().lock();
          try {
            managementCalls
                .stream()
                .filter(managementCall -> managementCall.getId().equals(((ManagementCallMessage) message).getManagementCallIdentifier()))
                .findFirst()
                .ifPresent(managementCall -> complete(managementCall, message.unwrap(ContextualReturn.class).get(0)));
          } finally {
            lock.readLock().unlock();
          }
          break;

        case "NOTIFICATION":
        case "STATISTICS":
          boolean offered = incomingMessageQueue.offer(Optional.of(message));
          if (!offered) {
            LOGGER.trace("Queue is full - Message lost: ", message);
          }
          break;

        default:
          LOGGER.warn("Received unsupported message: " + message);

      }
    });
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

  @Override
  public <T> ManagementCall<T> call(Context context, String capabilityName, String methodName, Class<T> returnType, Parameter... parameters) throws InterruptedException, ExecutionException, TimeoutException {
    LOGGER.trace("call({}, {}, {})", context, capabilityName, methodName);
    lock.writeLock().lock();
    try {
      String managementCallId = get(entity.call(null, context, capabilityName, methodName, returnType, parameters));
      VoltronManagementCall<T> managementCall = new VoltronManagementCall<>(managementCallId, context, returnType, timeout, managementCalls::remove);
      managementCalls.offer(managementCall);
      return managementCall;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void cancelAllManagementCalls() {
    while (!managementCalls.isEmpty()) {
      VoltronManagementCall<?> call = managementCalls.poll();
      if (call != null) { // can happen if list is cleared while iterating
        call.cancel();
      }
    }
  }

  private <V> V get(Future<V> future) throws ExecutionException, TimeoutException, InterruptedException {
    return future.get(timeout, TimeUnit.MILLISECONDS);
  }

  @SuppressWarnings("unchecked")
  private static <T> void complete(VoltronManagementCall<T> managementCall, ContextualReturn<?> aReturn) {
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

}
