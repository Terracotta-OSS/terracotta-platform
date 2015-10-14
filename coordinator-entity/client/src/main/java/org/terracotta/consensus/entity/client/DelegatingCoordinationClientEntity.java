/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.consensus.entity.client;

import org.terracotta.consensus.entity.Nomination;
import org.terracotta.entity.EndpointDelegate;
import org.terracotta.entity.EntityClientEndpoint;
import org.terracotta.voltron.proxy.ClientId;

/**
 * @author Alex Snaps
 */
public class DelegatingCoordinationClientEntity implements CoordinationClientEntity {

  private final CoordinationClientEntity target;
  private final EntityClientEndpoint entityClientEndpoint;

  public DelegatingCoordinationClientEntity(final CoordinationClientEntity entityProxy,
                                            final EntityClientEndpoint entityClientEndpoint) {
    target = entityProxy;
    this.entityClientEndpoint = entityClientEndpoint;
  }

  public Nomination runForElection(final String namespace, @ClientId final Object clientId) {
    return target.runForElection(namespace, clientId);
  }

  public void accept(final String namespace, final Nomination permit) {
    target.accept(namespace, permit);
  }

  public void delist(final String namespace, @ClientId final Object clientId) {
    target.delist(namespace, clientId);
  }

  public void close() {
    target.close();
  }

  public void registerListener(final Listener listener) {
    entityClientEndpoint.setDelegate(new EndpointDelegate() {
      public void handleMessage(final byte[] bytes) {
        // todo need to properly decode message TYPES here!
        listener.onElected(new String(bytes));
      }

      public byte[] createExtendedReconnectData() {
        // No idea! But don't think we share any state with the server
        return new byte[0];
      }

      public void didDisconnectUnexpectedly() {
        // No idea!
      }
    });
  }

  public interface Listener {
    void onElected(String namespace);
  }

}
