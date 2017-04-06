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
package org.terracotta.management.entity.nms.agent.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.management.entity.nms.agent.NmsAgentConfig;
import org.terracotta.management.entity.nms.agent.NmsAgentVersion;

/**
 * @author Mathieu Carbou
 */
public class NmsAgentEntityFactory {

  public static final String ENTITYNAME = "NmsAgent";

  private final Connection connection;

  public NmsAgentEntityFactory(Connection connection) {
    this.connection = connection;
  }

  public NmsAgentEntity retrieveOrCreate(NmsAgentConfig config) throws EntityConfigurationException {
    try {
      return retrieve();
    } catch (EntityNotFoundException e) {
      try {
        return create(config);
      } catch (EntityAlreadyExistsException f) {
        try {
          return retrieve();
        } catch (EntityNotFoundException e1) {
          throw new AssertionError(e);
        }
      }
    }
  }

  public NmsAgentEntity retrieve() throws EntityNotFoundException {
    try {
      return getEntityRef().fetchEntity(null);
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    }
  }

  public NmsAgentEntity create(NmsAgentConfig config) throws EntityAlreadyExistsException, EntityConfigurationException {
    EntityRef<NmsAgentEntity, NmsAgentConfig, Object> ref = getEntityRef();
    try {
      ref.create(config);
      return ref.fetchEntity(null);
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    } catch (EntityNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  private EntityRef<NmsAgentEntity, NmsAgentConfig, Object> getEntityRef() {
    try {
      return connection.getEntityRef(NmsAgentEntity.class, NmsAgentVersion.LATEST.version(), ENTITYNAME);
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    }
  }

}
