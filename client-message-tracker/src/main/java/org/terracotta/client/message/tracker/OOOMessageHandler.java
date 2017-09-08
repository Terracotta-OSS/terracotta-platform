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
package org.terracotta.client.message.tracker;

import org.terracotta.entity.ClientSourceId;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpable;

import com.tc.classloader.CommonComponent;

import java.util.Map;
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
   * Get all message id - response mappings for the given {@code clientSourceId} in the given segment
   *
   * @param index the segment index
   * @param clientSourceId a client descriptor
   * @return a map with message id - response mappings
   */
  Map<Long, R> getTrackedResponsesForSegment(int index, ClientSourceId clientSourceId);

  /**
   * Bulk load a set of message ids, response mappings for the given client descriptor  in the given segment.
   * To be used by a passive entity when the active syncs its message tracker data.
   *
   * @param index a segment index
   * @param clientSourceId a client descriptor
   * @param trackedResponses a map of message id, response mappings
   */
  void loadTrackedResponsesForSegment(int index, ClientSourceId clientSourceId, Map<Long, R> trackedResponses);

  /**
   * Bulk load a set of message ids, response mappings for the given client descriptor.
   * To be used by a passive entity when the active syncs its message tracker data.
   * The mappings loaded by this message will go to any of the segments but goes to a
   * special tracker that is common between all segments.
   *
   * @param clientSourceId a client descriptor
   * @param trackedResponses a map of message id, response mappings
   */
  @Deprecated
  void loadOnSync(ClientSourceId clientSourceId, Map<Long, R> trackedResponses);
}
