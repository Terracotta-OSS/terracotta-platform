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
package org.terracotta.management.entity.nms.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;
import org.terracotta.management.entity.nms.NmsConfig;
import org.terracotta.management.entity.nms.NmsVersion;

/**
 * @author Mathieu Carbou
 */
public class NmsEntityFactory {

  private final Connection connection;
  private final String entityName;

  public NmsEntityFactory(Connection connection, String entityName) {
    this.connection = connection;
    this.entityName = entityName;
  }

  public NmsEntity retrieveOrCreate(NmsConfig config) throws EntityConfigurationException {
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

  public NmsEntity retrieve() throws EntityNotFoundException {
    try {
      return getEntityRef().fetchEntity(null);
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    }
  }

  public NmsEntity create(NmsConfig config) throws EntityAlreadyExistsException, EntityConfigurationException {
    EntityRef<NmsEntity, NmsConfig, Object> ref = getEntityRef();
    try {
      ref.create(config);
      return ref.fetchEntity(null);
    } catch (EntityNotProvidedException | EntityVersionMismatchException | EntityNotFoundException e) {
      throw new AssertionError(e);
    }
  }

  private EntityRef<NmsEntity, NmsConfig, Object> getEntityRef() {
    try {
      return connection.getEntityRef(NmsEntity.class, NmsVersion.LATEST.version(), entityName);
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    }
  }

}
