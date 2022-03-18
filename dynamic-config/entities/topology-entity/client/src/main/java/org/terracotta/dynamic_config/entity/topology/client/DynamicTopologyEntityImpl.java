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
package org.terracotta.dynamic_config.entity.topology.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.entity.topology.common.Message;
import org.terracotta.dynamic_config.entity.topology.common.Response;
import org.terracotta.dynamic_config.entity.topology.common.Type;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_HAS_INCOMPLETE_CHANGE;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_LICENSE;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_MUST_BE_RESTARTED;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_RUNTIME_CLUSTER;
import static org.terracotta.dynamic_config.entity.topology.common.Type.REQ_UPCOMING_CLUSTER;

/**
 * @author Mathieu Carbou
 */
class DynamicTopologyEntityImpl implements DynamicTopologyEntity {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTopologyEntityImpl.class);

  private final EntityClientEndpoint<Message, Response> endpoint;
  private final Settings settings;

  private volatile Listener listener = new Listener() {};

  public DynamicTopologyEntityImpl(EntityClientEndpoint<Message, Response> endpoint, Settings settings) {
    this.endpoint = endpoint;
    this.settings = settings == null ? new Settings() : settings;

    endpoint.setDelegate(new EndpointDelegate<Response>() {
      @Override
      public void handleMessage(Response messageFromServer) {
        try {
          LOGGER.trace("handleMessage({})", messageFromServer);
          switch (messageFromServer.getType()) {
            case EVENT_NODE_ADDITION: {
              List<Object> payload = messageFromServer.getPayload();
              listener.onNodeAddition((Cluster) payload.get(0), (UID) payload.get(1));
              break;
            }
            case EVENT_NODE_REMOVAL: {
              List<Object> payload = messageFromServer.getPayload();
              listener.onNodeRemoval((Cluster) payload.get(0), (UID) payload.get(1), (Node) payload.get(2));
              break;
            }
            case EVENT_SETTING_CHANGED: {
              List<Object> payload = messageFromServer.getPayload();
              listener.onSettingChange((Cluster) payload.get(0), (Configuration) payload.get(1));
              break;
            }
            case EVENT_STRIPE_ADDITION: {
              List<Object> payload = messageFromServer.getPayload();
              listener.onStripeAddition((Cluster) payload.get(0), (UID) payload.get(1));
              break;
            }
            case EVENT_STRIPE_REMOVAL: {
              List<Object> payload = messageFromServer.getPayload();
              listener.onStripeRemoval((Cluster) payload.get(0), (Stripe) payload.get(1));
              break;
            }
            default:
              throw new AssertionError(messageFromServer);
          }
        } catch (RuntimeException e) {
          LOGGER.error("Error handling message: " + messageFromServer + ": " + e.getMessage(), e);
        }
      }

      @Override
      public byte[] createExtendedReconnectData() {
        return new byte[0];
      }

      @Override
      public void didDisconnectUnexpectedly() {
        listener.onDisconnected();
      }
    });
  }

  @Override
  public void close() {
    endpoint.close();
  }

  @Override
  public void setListener(Listener listener) {
    this.listener = listener == null ? new Listener() {} : listener;
  }

  @Override
  public Cluster getUpcomingCluster() throws TimeoutException, InterruptedException {
    return request(REQ_UPCOMING_CLUSTER, Cluster.class);
  }

  @Override
  public Cluster getRuntimeCluster() throws TimeoutException, InterruptedException {
    return request(REQ_RUNTIME_CLUSTER, Cluster.class);
  }

  @Override
  public boolean mustBeRestarted() throws TimeoutException, InterruptedException {
    return request(REQ_MUST_BE_RESTARTED, boolean.class);
  }

  @Override
  public boolean hasIncompleteChange() throws TimeoutException, InterruptedException {
    return request(REQ_HAS_INCOMPLETE_CHANGE, boolean.class);
  }

  @Override
  public License getLicense() throws TimeoutException, InterruptedException {
    return request(REQ_LICENSE, License.class);
  }

  @Override
  public Future<Void> releaseEntity() {
    return endpoint.release();
  }

  public <T> T request(Type messageType, Class<T> type) throws TimeoutException, InterruptedException {
    LOGGER.trace("request({})", messageType);
    Duration requestTimeout = settings.getRequestTimeout();
    try {
      InvokeFuture<Response> invoke = endpoint.beginInvoke()
          .message(new Message(messageType))
          .invoke();
      Response response = (requestTimeout == null ? invoke.get() : invoke.getWithTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS));
      LOGGER.trace("response({})", response);
      return type.cast(response.getPayload());
    } catch (MessageCodecException | EntityException e) {
      throw new AssertionError(e); // programming error
    }
  }
}
