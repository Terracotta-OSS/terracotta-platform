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
package org.terracotta.management.service.monitoring.platform;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

/**
 * The interface which must be implemented by a monitoring component in order to receive the data entities passed into
 * IMonitoringProducer, on a server within the stripe.
 * <p>
 * Note that only the implementation on the current active server will receive this data but it will receive the data from
 * the entire stripe.
 * <p>
 * Note that the values used in these methods are Serializable since they may have come over the wire.
 */
@CommonComponent
public interface IStripeMonitoring {

  /**
   * Called when a server first becomes active to notify its IStripeMonitoring implementation that it will now start to
   * receive the other calls in this interface.  The PlatformServer representing itself is provided so that it can identify
   * the calls which are locally-originating.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param self The description of the active server where this call occurs.
   */
  void serverDidBecomeActive(PlatformServer self);

  /**
   * Called to notify the implementation when another server has first joined the stripe, meaning that messages may start
   * arriving from this server.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param server The description of the newly-arrived server.
   */
  void serverDidJoinStripe(PlatformServer server);

  /**
   * Called to notify the implementation when another server has left the stripe, meaning that no more messages will be
   * arriving from this server and any others from it are now stale.
   * NOTE:  This is only ever called on the consumerID 0 instance.
   *
   * @param server The description of the now-departed server.
   */
  void serverDidLeaveStripe(PlatformServer server);

  void serverEntityCreated(PlatformServer sender, PlatformEntity entity);

  void serverEntityDestroyed(PlatformServer sender, PlatformEntity entity);

  void serverStateChanged(PlatformServer sender, ServerState state);

  void clientConnected(PlatformConnectedClient client);

  void clientDisconnected(PlatformConnectedClient client);

  void clientFetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

  void clientUnfetch(PlatformConnectedClient client, PlatformEntity entity, ClientDescriptor clientDescriptor);

}
