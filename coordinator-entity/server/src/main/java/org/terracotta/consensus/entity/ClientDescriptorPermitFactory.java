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

import org.terracotta.consensus.entity.server.PermitFactory;
import org.terracotta.entity.ClientDescriptor;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Alex Snaps
 */
public class ClientDescriptorPermitFactory implements PermitFactory<ClientDescriptor> {

  private static final AtomicLong counter = new AtomicLong();

  public Nomination createPermit(final ClientDescriptor clientDescriptor) {
    return new Nomination(counter.getAndIncrement());
  }
}
