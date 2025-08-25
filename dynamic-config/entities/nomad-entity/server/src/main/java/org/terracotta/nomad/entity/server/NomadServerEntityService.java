/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.nomad.entity.server;

import com.tc.classloader.PermanentEntity;
import org.terracotta.dynamic_config.server.api.DynamicConfigNomadServer;
import org.terracotta.entity.BasicServiceConfiguration;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityServerService;
import org.terracotta.entity.ExecutionStrategy;
import org.terracotta.entity.MessageCodecException;
import org.terracotta.entity.ServiceException;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;
import org.terracotta.nomad.entity.common.NomadEntityConstants;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.entity.common.NomadMessageCodec;
import org.terracotta.server.Server;

@PermanentEntity(type = NomadEntityConstants.ENTITY_TYPE, name = NomadEntityConstants.ENTITY_NAME)
public class NomadServerEntityService<T> implements EntityServerService<NomadEntityMessage, NomadEntityResponse> {

  private final NomadMessageCodec messageCodec = new NomadMessageCodec();

  @Override
  public NomadActiveServerEntity<T> createActiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      DynamicConfigNomadServer nomadServer = registry.getService(new BasicServiceConfiguration<>(DynamicConfigNomadServer.class));
      return new NomadActiveServerEntity<>(nomadServer);
    } catch (ServiceException e) {
      throw new ConfigurationException("Could not retrieve service ", e);
    }
  }

  @Override
  public NomadPassiveServerEntity<T> createPassiveEntity(ServiceRegistry registry, byte[] configuration) throws ConfigurationException {
    try {
      DynamicConfigNomadServer nomadServer = registry.getService(new BasicServiceConfiguration<>(DynamicConfigNomadServer.class));
      Server server = registry.getService(new BasicServiceConfiguration<>(Server.class));
      return new NomadPassiveServerEntity<>(server, nomadServer);
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
    return NomadEntityConstants.ENTITY_TYPE.equals(typeName);
  }
}
