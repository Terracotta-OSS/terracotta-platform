/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.server;

import com.tc.classloader.PermanentEntity;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.monitoring.PlatformService;
import org.terracotta.nomad.entity.common.Constants;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.entity.common.NomadMessageCodec;
import org.terracotta.nomad.server.NomadServer;

@PermanentEntity(type = Constants.ENTITY_TYPE, name = Constants.ENTITY_NAME)
public class NomadServerEntityService<T> implements EntityServerService<NomadEntityMessage, NomadEntityResponse> {

  private final NomadMessageCodec messageCodec = new NomadMessageCodec();

  @Override
  public NomadActiveServerEntity<T> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      @SuppressWarnings("unchecked")
      NomadServer<T> nomadServer = registry.getService(new BasicServiceConfiguration<>(NomadServer.class));
      return new NomadActiveServerEntity<>(nomadServer);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public NomadPassiveServerEntity<T> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      @SuppressWarnings("unchecked")
      NomadServer<T> nomadServer = registry.getService(new BasicServiceConfiguration<>(NomadServer.class));
      PlatformService platformService = registry.getService(new BasicServiceConfiguration<>(PlatformService.class));
      return new NomadPassiveServerEntity<>(nomadServer, platformService);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public NomadMessageCodec getMessageCodec() {
    return messageCodec;
  }

  @Override
  public SyncMessageCodec<NomadEntityMessage> getSyncMessageCodec() {
    return new SyncMessageCodec<NomadEntityMessage>() {
      @Override
      public byte[] encode(int concurrencyKey, NomadEntityMessage response) throws MessageCodecException {
        return getMessageCodec().encodeMessage(response);
      }

      @Override
      public NomadEntityMessage decode(int concurrencyKey, byte[] payload) throws MessageCodecException {
        return getMessageCodec().decodeMessage(payload);
      }
    };
  }

  // we do not want Nomad calls to be done concurrently, same when a passive tries to sync from an active
  @Override
  public ConcurrencyStrategy<NomadEntityMessage> getConcurrencyStrategy(byte[] configuration) {
    return new NoConcurrency();
  }

  // we want messages to be sent both to active and passive entities
  @Override
  public ExecutionStrategy<NomadEntityMessage> getExecutionStrategy(byte[] configuration) {
    return message -> ExecutionStrategy.Location.BOTH;
  }

  @Override
  public long getVersion() {
    return 1;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return Constants.ENTITY_TYPE.equals(typeName);
  }
}
