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
package org.terracotta.management.entity.management.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.management.entity.management.ManagementAgentConfig;
import org.terracotta.management.entity.management.ManagementAgentVersion;

/**
 * @author Mathieu Carbou
 */
public class ManagementAgentEntityFactory {

  public static final String ENTITYNAME = "ManagementAgent";

  private final Connection connection;

  public ManagementAgentEntityFactory(Connection connection) {
    this.connection = connection;
  }

  public ManagementAgentEntity retrieveOrCreate(ManagementAgentConfig config) {
    try {
      return retrieve();
    } catch (EntityNotFoundException e) {
      try {
        return create(config);
      } catch (EntityAlreadyExistsException f) {
        throw new AssertionError(e);
      }
    }
  }

  public ManagementAgentEntity retrieve() throws EntityNotFoundException {
    try {
      return getEntityRef().fetchEntity();
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    }
  }

  public ManagementAgentEntity create(ManagementAgentConfig config) throws EntityAlreadyExistsException {
    EntityRef<ManagementAgentEntity, ManagementAgentConfig> ref = getEntityRef();
    try {
      ref.create(config);
      return ref.fetchEntity();
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    } catch (EntityNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  private EntityRef<ManagementAgentEntity, ManagementAgentConfig> getEntityRef() {
    try {
      return connection.getEntityRef(ManagementAgentEntity.class, ManagementAgentVersion.LATEST.version(), ENTITYNAME);
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    }
  }

}
