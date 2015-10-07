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

import org.terracotta.consensus.entity.server.LeaderElectorImpl;
import org.terracotta.consensus.entity.server.PermitFactory;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.voltron.proxy.ClientId;
import org.terracotta.voltron.proxy.SerializationCodec;
import org.terracotta.voltron.proxy.server.ProxyInvoker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alex Snaps
 */
public class CoordinationServerEntity implements ActiveServerEntity {

  private final LeaderElectorImpl<String, ClientDescriptor> leaderElector = new LeaderElectorImpl<String, ClientDescriptor>(new ClientDescriptorPermitFactory());
  private final ProxyInvoker target = new ProxyInvoker(CoordinationEntity.class, new CoordinationEntityImpl(), new SerializationCodec());

  public byte[] invoke(final ClientDescriptor clientDescriptor, final byte[] arg) {
    return target.invoke(clientDescriptor, arg);
  }

  public ConcurrencyStrategy getConcurrencyStrategy() {
    return new NoConcurrencyStrategy();
  }

  public void connected(final ClientDescriptor clientDescriptor) {
    // no op
  }

  public void disconnected(final ClientDescriptor clientDescriptor) {
    leaderElector.delistAll(clientDescriptor);
  }

  public byte[] getConfig() {
    return null;
  }

  public void handleReconnect(final ClientDescriptor clientDescriptor, final byte[] bytes) {
    // Don't care I think
  }

  public void createNew() {
    // Don't care I think
  }

  public void loadExisting() {
    // Don't care I think
  }

  public void destroy() {
    // Don't care I think
  }

  private static class CoordinationEntityImpl implements CoordinationEntity {
    public Nomination runForElection(final String namespace, @ClientId final Object clientId) {
      throw new UnsupportedOperationException("Implement me!");
    }

    public void accept(final String namespace, final Nomination permit) {
      throw new UnsupportedOperationException("Implement me!");
    }

    public void delist(final String namespace, @ClientId final Object clientId) {
      throw new UnsupportedOperationException("Implement me!");
    }
  }

  private static class ClientDescriptorPermitFactory implements PermitFactory<ClientDescriptor> {

    private AtomicLong counter = new AtomicLong();

    public Object createPermit(final ClientDescriptor clientDescriptor) {
      return new Nomination(counter.getAndIncrement());
    }
  }
}
