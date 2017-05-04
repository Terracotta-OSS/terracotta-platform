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

import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.message.Message;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;

/**
 * @author Anthony Dahanne
 */
public interface NmsService {

  Comparator<Message> MESSAGE_COMPARATOR = Comparator.comparing(Message::getSequence);

  NmsService setOperationTimeout(long duration, TimeUnit unit);

  Cluster readTopology() throws TimeoutException, InterruptedException, ExecutionException;

  /**
   * Wait for a message to arrive in the queue
   */
  Message waitForMessage() throws InterruptedException;

  /**
   * Wait for a message to arrive in the queue for a maximum amount of time
   */
  Message waitForMessage(long time, TimeUnit unit) throws InterruptedException, TimeoutException;

  /**
   * Drain all messages received in the queue. Drained messages are ordered by their sequence
   */
  List<Message> readMessages();

  /**
   * Wait for a message until the predicate returns true and returns the collected messages during this time, sorted by sequence
   *
   * @param predicate A function that takes as a parameter the newly received message
   * @throws InterruptedException In case the call is interrupted
   */
  default List<Message> waitForMessage(Predicate<Message> predicate) throws InterruptedException {
    List<Message> collected = new ArrayList<>();
    Message message;
    do {
      message = waitForMessage();
      collected.add(message);
    }
    while (!predicate.test(message));
    collected.sort(MESSAGE_COMPARATOR);
    return collected;
  }

  default ManagementCall<Void> startStatisticCollector(Context context, long interval, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    return call(
        context,
        "StatisticCollectorCapability",
        "startStatisticCollector",
        Void.TYPE,
        new Parameter(interval, long.class.getName()),
        new Parameter(unit, TimeUnit.class.getName()));
  }

  default ManagementCall<Void> stopStatisticCollector(Context context) throws InterruptedException, ExecutionException, TimeoutException {
    return call(
        context,
        "StatisticCollectorCapability",
        "stopStatisticCollector",
        Void.TYPE);
  }

  <T> ManagementCall<T> call(Context context, String capabilityName, String methodName, Class<T> returnType, Parameter... parameters) throws InterruptedException, ExecutionException, TimeoutException;

  void cancelAllManagementCalls();
}
