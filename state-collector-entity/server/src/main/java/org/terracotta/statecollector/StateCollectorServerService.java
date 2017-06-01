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
package org.terracotta.statecollector;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.NoConcurrencyStrategy;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.monitoring.PlatformService;

import com.tc.classloader.PermanentEntity;

@PermanentEntity(type = StateCollectorConstants.STATE_COLLECTOR_CLASS_NAME, names={ StateCollectorConstants.STATE_COLLECTOR_ENTITY_NAME }, version = StateCollectorConstants.STATE_COLLECTOR_VERSION)
public class StateCollectorServerService implements EntityServerService<StateCollectorMessage, StateCollectorMessage> {
  @Override
  public long getVersion() {
    return StateCollectorConstants.STATE_COLLECTOR_VERSION;
  }

  @Override
  public boolean handlesEntityType(final String s) {
    return StateCollectorConstants.STATE_COLLECTOR_CLASS_NAME.equals(s);
  }

  @Override
  public ActiveServerEntity<StateCollectorMessage, StateCollectorMessage> createActiveEntity(final ServiceRegistry serviceRegistry, final byte[] bytes) throws ConfigurationException {
    try {
      ClientCommunicator clientCommunicator = serviceRegistry.getService(new BasicServiceConfiguration<>(ClientCommunicator.class));
      return new StateCollectorActive(clientCommunicator);
    } catch (ServiceException e) {
      throw new RuntimeException("Couldn't fetch service", e);
    }
  }

  @Override
  public PassiveServerEntity<StateCollectorMessage, StateCollectorMessage> createPassiveEntity(final ServiceRegistry serviceRegistry, final byte[] bytes) throws ConfigurationException {
    return new PassiveServerEntity<StateCollectorMessage, StateCollectorMessage>() {
      @Override
      public void invoke(final StateCollectorMessage stateCollectorMessage) throws EntityUserException {
        //no invokes expected
      }

      @Override
      public void startSyncEntity() {
        //nothing to do
      }

      @Override
      public void endSyncEntity() {
        //nothing to do
      }

      @Override
      public void startSyncConcurrencyKey(final int i) {
        //nothing to do
      }

      @Override
      public void endSyncConcurrencyKey(final int i) {
        //nothing to do
      }

      @Override
      public void createNew() throws ConfigurationException {
        //nothing to do
      }

      @Override
      public void destroy() {
        //nothing to do
      }
    };
  }

  @Override
  public ConcurrencyStrategy<StateCollectorMessage> getConcurrencyStrategy(final byte[] bytes) {
    return new NoConcurrencyStrategy<>();
  }

  @Override
  public MessageCodec<StateCollectorMessage, StateCollectorMessage> getMessageCodec() {
    return new StateCollectorCodec();
  }

  @Override
  public SyncMessageCodec<StateCollectorMessage> getSyncMessageCodec() {
    return null;
  }
}
