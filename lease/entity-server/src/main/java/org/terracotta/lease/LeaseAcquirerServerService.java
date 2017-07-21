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

import com.tc.classloader.PermanentEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ClientCommunicator;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.IEntityMessenger;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.lease.service.LeaseService;
import org.terracotta.lease.service.LeaseServiceConfiguration;
import org.terracotta.lease.service.closer.ClientConnectionCloser;
import org.terracotta.lease.service.closer.ClientConnectionCloserImpl;

import java.util.Collections;
import java.util.Set;

import static org.terracotta.lease.LeaseEntityConstants.ENTITY_NAME;
import static org.terracotta.lease.LeaseEntityConstants.ENTITY_VERSION;

/**
 * The object that creates the active and passive entities for the connection leasing.
 */
@PermanentEntity(type = "org.terracotta.lease.LeaseAcquirer", names = {ENTITY_NAME}, version = ENTITY_VERSION)
public class LeaseAcquirerServerService implements EntityServerService<LeaseMessage, LeaseResponse> {
  private static final Logger LOGGER = LoggerFactory.getLogger(LeaseAcquirerServerService.class);

  @Override
  public long getVersion() {
    return 1L;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "org.terracotta.lease.LeaseAcquirer".equals(typeName);
  }

  @Override
  public ActiveServerEntity<LeaseMessage, LeaseResponse> createActiveEntity(ServiceRegistry serviceRegistry, byte[] bytes) throws ConfigurationException {
    ClientCommunicator clientCommunicator = getService(serviceRegistry, new BasicServiceConfiguration<>(ClientCommunicator.class));

    ClientConnectionCloser clientConnectionCloser = new ClientConnectionCloserImpl(clientCommunicator);
    LeaseServiceConfiguration leaseServiceConfiguration = new LeaseServiceConfiguration(clientConnectionCloser);
    LeaseService leaseService = getService(serviceRegistry, leaseServiceConfiguration);

    IEntityMessenger entityMessenger = getService(serviceRegistry, new BasicServiceConfiguration<>(IEntityMessenger.class));

    return new ActiveLeaseAcquirer(leaseService, clientCommunicator, entityMessenger);
  }

  private <T> T getService(ServiceRegistry serviceRegistry, ServiceConfiguration<T> serviceConfiguration) throws ConfigurationException {
    try {
      T service = serviceRegistry.getService(serviceConfiguration);

      if (service == null) {
        LOGGER.error("Missing service: " + serviceConfiguration.getServiceType());
        throw new ConfigurationException("Missing service: " + serviceConfiguration.getServiceType());
      }

      return service;
    } catch (ServiceException e) {
      LOGGER.error("Too many services of type: " + serviceConfiguration.getServiceType() + ", expected 1");
      throw new ConfigurationException("Too many services of type: " + serviceConfiguration.getServiceType() + ", expected 1", e);
    }
  }

  @Override
  public PassiveServerEntity<LeaseMessage, LeaseResponse> createPassiveEntity(ServiceRegistry serviceRegistry, byte[] bytes) throws ConfigurationException {
    return new PassiveLeaseAcquirer();
  }

  @Override
  public ConcurrencyStrategy<LeaseMessage> getConcurrencyStrategy(byte[] bytes) {
    return new ConcurrencyStrategy<LeaseMessage>() {
      @Override
      public int concurrencyKey(LeaseMessage leaseMessage) {
        LeaseMessageType messageType = leaseMessage.getType();
        switch (messageType) {
          case LEASE_REQUEST:
            return ConcurrencyStrategy.UNIVERSAL_KEY;
          case LEASE_RECONNECT_FINISHED:
            return ConcurrencyStrategy.MANAGEMENT_KEY;
          default:
            throw new AssertionError("Unknown LeaseMessage type: " + messageType);
        }
      }

      @Override
      public Set<Integer> getKeysForSynchronization() {
        return Collections.emptySet();
      }
    };
  }

  @Override
  public MessageCodec<LeaseMessage, LeaseResponse> getMessageCodec() {
    return new LeaseAcquirerCodec();
  }

  @Override
  public SyncMessageCodec<LeaseMessage> getSyncMessageCodec() {
    return null;
  }
}
