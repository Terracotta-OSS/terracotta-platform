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

package org.terracotta.consensus.entity;

import java.util.UUID;

import org.terracotta.entity.ClientDescriptor;
import org.terracotta.consensus.entity.server.OfferFactory;

/**
 * @author Alex Snaps
 */
public class UuidOfferFactory implements OfferFactory<ClientDescriptor> {

  public LeaderOffer createOffer(final ClientDescriptor client, boolean clean) {
    return new UuidLeaderOffer(clean, UUID.randomUUID());
  }

  private static class UuidLeaderOffer extends LeaderOffer {

    private final UUID offerId;

    public UuidLeaderOffer(boolean clean, UUID offerId) {
      super(clean);
      this.offerId = offerId;
    }

    @Override
    public int hashCode() {
      return offerId.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
      return (obj instanceof UuidLeaderOffer) && offerId.equals(((UuidLeaderOffer) obj).offerId);
    }
  }
}
