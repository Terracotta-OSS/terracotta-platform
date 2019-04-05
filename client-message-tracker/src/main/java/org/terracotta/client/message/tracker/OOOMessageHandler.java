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

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.StateDumpable;

import java.util.function.BiFunction;

/**
 * Entities that want once and only once message invocation guarantees can delegate their invokes to this service.
 */
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
   * @param clientDescriptor the client descriptor of the disconnected client
   */
  void untrackClient(ClientDescriptor clientDescriptor);

  /**
   * Destroys the {@code OOOMessageHandler}
   */
  void destroy();

  /**
   * Callback to be called when the handler is destroyed.
   */
  interface DestroyCallback {
    /**
     * The cleanup action required to be done on destruction
     */
    void destroy();
  }
}
