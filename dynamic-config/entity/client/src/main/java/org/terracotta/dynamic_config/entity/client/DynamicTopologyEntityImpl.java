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
package org.terracotta.dynamic_config.entity.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.entity.InvokeFuture;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.exception.EntityException;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.REQ_HAS_INCOMPLETE_CHANGE;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.REQ_LICENSE;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.REQ_MUST_BE_RESTARTED;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.REQ_RUNTIME_CLUSTER;
import static org.terracotta.dynamic_config.entity.common.DynamicTopologyEntityMessage.Type.REQ_UPCOMING_CLUSTER;

/**
 * @author Mathieu Carbou
 */
class DynamicTopologyEntityImpl implements DynamicTopologyEntity {
  private static final Logger LOGGER = LoggerFactory.getLogger(DynamicTopologyEntityImpl.class);

  private final EntityClientEndpoint<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> endpoint;
  private final Settings settings;

  private volatile Listener listener = new Listener() {};

  public DynamicTopologyEntityImpl(EntityClientEndpoint<DynamicTopologyEntityMessage, DynamicTopologyEntityMessage> endpoint, Settings settings) {
    this.endpoint = endpoint;
    this.settings = settings == null ? new Settings() : settings;

    endpoint.setDelegate(new EndpointDelegate<DynamicTopologyEntityMessage>() {
      @Override
      public void handleMessage(DynamicTopologyEntityMessage messageFromServer) {
        switch (messageFromServer.getType()) {
          case EVENT_NODE_ADDITION: {
            Object[] payload = (Object[]) messageFromServer.getPayload();
            listener.onNodeAddition((int) payload[0], (Node) payload[1]);
            break;
          }
          case EVENT_NODE_REMOVAL: {
            Object[] payload = (Object[]) messageFromServer.getPayload();
            listener.onNodeRemoval((int) payload[0], (Node) payload[1]);
            break;
          }
          case EVENT_SETTING_CHANGED: {
            Object[] payload = (Object[]) messageFromServer.getPayload();
            listener.onSettingChange((Configuration) payload[0], (Cluster) payload[1]);
            break;
          }
          default:
            throw new AssertionError(messageFromServer);
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

  public <T> T request(DynamicTopologyEntityMessage.Type messageType, Class<T> type) throws TimeoutException, InterruptedException {
    LOGGER.trace("request({})", messageType);
    Duration requestTimeout = settings.getRequestTimeout();
    try {
      InvokeFuture<DynamicTopologyEntityMessage> invoke = endpoint.beginInvoke()
          .message(new DynamicTopologyEntityMessage(messageType))
          .invoke();
      DynamicTopologyEntityMessage response = (requestTimeout == null ? invoke.get() : invoke.getWithTimeout(requestTimeout.toMillis(), TimeUnit.MILLISECONDS));
      LOGGER.trace("response({})", response);
      return type.cast(response.getPayload());
    } catch (MessageCodecException | EntityException e) {
      throw new AssertionError(e); // programming error
    }
  }
}
