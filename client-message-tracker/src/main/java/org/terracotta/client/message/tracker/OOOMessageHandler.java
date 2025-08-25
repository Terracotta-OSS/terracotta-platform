/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpable;

import com.tc.classloader.CommonComponent;

import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Entities that want once and only once message invocation guarantees can delegate their invokes to this service.
 */
@CommonComponent
public interface OOOMessageHandler<M extends EntityMessage, R extends EntityResponse> extends StateDumpable {

  /**
   * Will return a cached response from a previous invocation of the same {@code message} with the given {@code context}
   * if one existed. Else will apply {@code invokeFunction} and return the response after caching it.
   *
   * @param context invocation context
   * @param message a message
   * @param invokeFunction the entity invocation logic
   * @return a cached response if one is available, else the result of the {@code invokeFunction}
   * @throws EntityUserException
   */
  R invoke(InvokeContext context, M message, BiFunction<InvokeContext, M, R> invokeFunction) throws EntityUserException;

  /**
   * Notify that a client has disconnected from the cluster and all knowledge about that client can be cleared.
   *
   * @param clientSourceId the client descriptor of the disconnected client
   */
  void untrackClient(ClientSourceId clientSourceId);

  /**
   * Return ids of all the client sources tracked by this handler.
   *
   * @return a stream of tracked client sources
   */
  Stream<ClientSourceId> getTrackedClients();
  /**
   * lookup of duplicates has closed and will no longer occur
   */
  void closeDuplicatesWindow();
  /**
   * Lookup a response for a transaction for a client on a particular segment.
   *
   * @param src client source of the transaction
   * @param txn the transaction id of the message
   * @return response for the tracked message if available
   */
  R lookupResponse(ClientSourceId src, long txn);

  /**
   * Get a stream of tracked messages ordered by sequence id - Order is important so replay is
   * sequenced correctly.
   *
   * @return a stream of ordered RecordedMessages
   */
  Stream<RecordedMessage<M, R>> getRecordedMessages();

  /**
   * load all the sequenced messages to the current message tracker
   * 
   * @param recorded - a stream of recorded messages
   */
  void loadRecordedMessages(Stream<RecordedMessage<M, R>> recorded);
  /**
   * Destroys the {@code OOOMessageHandler}
   */
  void destroy();

  /**
   * Callback to be called when the handler is destroyed.
   */
  @CommonComponent
  interface DestroyCallback {
    /**
     * The cleanup action required to be done on destruction
     */
    void destroy();
  }
}
