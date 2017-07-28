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
package org.terracotta.voltron.proxy.server;

import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.StateDumpCollector;
import org.terracotta.voltron.proxy.ProxyEntityMessage;
import org.terracotta.voltron.proxy.ProxyEntityResponse;

/**
 * @author Mathieu Carbou
 */
public abstract class PassiveProxiedServerEntity implements PassiveServerEntity<ProxyEntityMessage, ProxyEntityResponse> {

  private final ProxyInvoker<?> entityInvoker = new ProxyInvoker<>(this);

  @Override
  public void invokePassive(InvokeContext context, ProxyEntityMessage message) throws EntityUserException {
    switch (message.getType()) {
      case SYNC:
      case MESSENGER:
      case MESSAGE:
        entityInvoker.invoke(message);
        break;
      default:
        throw new AssertionError(message.getType());
    }
  }

  @Override
  public void startSyncEntity() {

  }

  @Override
  public void endSyncEntity() {

  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {

  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {

  }

  @Override
  public void createNew() {
    // Don't care I think
  }

  @Override
  public void destroy() {
    // Don't care I think
  }

  @Override
  public final void addStateTo(StateDumpCollector stateDumpCollector) {
    stateDumpCollector.addState("instance", this.toString());
    dumpState(stateDumpCollector);
  }

  protected void dumpState(StateDumpCollector stateDumpCollector) {
  }

}
