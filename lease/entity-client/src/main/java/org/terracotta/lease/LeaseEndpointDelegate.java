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
package org.terracotta.lease;

import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityResponse;

class LeaseEndpointDelegate implements EndpointDelegate<LeaseResponse> {
  private final LeaseReconnectListener reconnectListener;
  private final LeaseReconnectDataSupplier reconnectDataSupplier;

  public LeaseEndpointDelegate(LeaseReconnectListener reconnectListener, LeaseReconnectDataSupplier reconnectDataSupplier) {
    this.reconnectListener = reconnectListener;
    this.reconnectDataSupplier = reconnectDataSupplier;
  }

  @Override
  public void handleMessage(LeaseResponse entityResponse) {
    if (!(entityResponse instanceof LeaseAcquirerAvailable)) {
      throw new AssertionError("Received unexpected message from server: " + entityResponse);
    }

    reconnectListener.reconnected();
  }

  @Override
  public byte[] createExtendedReconnectData() {
    reconnectListener.reconnecting();
    LeaseReconnectData reconnectData = reconnectDataSupplier.getReconnectData();
    return reconnectData.encode();
  }

  @Override
  public void didDisconnectUnexpectedly() {
  }
}
