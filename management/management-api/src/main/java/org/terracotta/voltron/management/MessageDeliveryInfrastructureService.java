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
package org.terracotta.voltron.management;

import org.terracotta.voltron.management.consumer.MessageConsumerListener;
import org.terracotta.voltron.management.producer.MessageProducer;

/**
 * The message delivery infrastructure. Handles creation of producers and consumers for
 * one or more type(s) of messages.
 *
 * @author RKAV
 */
public interface MessageDeliveryInfrastructureService {
  /**
   * Create the Message producer for a given message type. Allows managed entities
   * to push messages to any interested management entities.
   * <p>
   * For thread safety reasons, each producer thread in a managed entity must create
   * a separate message producer and use it.
   *
   * @param messageType the type of messages that will be produced. This could be simple
   *                    unserialized byte arrays that are coming from the client(s)
   *                    or full contextual statistics created by the stripe themselves or
   *                    anything else for that matter.
   *
   * @return a message producer
   */
  <M> MessageProducer<M> createMessageProducer(Class<M> messageType);

  /**
   * Allows consumers (typically management entities) to get a message consumer
   * for consuming messages (such as statistics) that are coming from managed
   * entities.
   *
   * @param messageType the type of messages that will be consumed by this consumer.
   * @param messageConsumerListener Listener interface to pass on a message consumer
   *                                that can be used to consume messages coming from one or
   *                                more managed entities.
   */
  <M> void registerMessageConsumerListener(Class<M> messageType,
                                           MessageConsumerListener<M> messageConsumerListener);
}
