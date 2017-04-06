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
package org.terracotta.management.entity.sample.client;

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityConfigurationException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

/**
 * @author Mathieu Carbou
 */
public class CacheEntityFactory {

  private final Connection connection;

  public CacheEntityFactory(Connection connection) {
    this.connection = connection;
  }

  public CacheEntity retrieveOrCreate(String entityName, String cacheName) {
    try {
      return retrieve(entityName);
    } catch (EntityNotFoundException e) {
      try {
        return create(entityName, cacheName);
      } catch (EntityAlreadyExistsException f) {
        throw new AssertionError(e);
      }
    }
  }

  public CacheEntity retrieve(String entityName) throws EntityNotFoundException {
    try {
      return getEntityRef(entityName).fetchEntity(null);
    } catch (EntityVersionMismatchException e) {
      throw new AssertionError(e);
    }
  }

  public CacheEntity create(String entityName, String cacheName) throws EntityAlreadyExistsException {
    EntityRef<CacheEntity, String, Object> ref = getEntityRef(entityName);
    try {
      ref.create(cacheName);
      return ref.fetchEntity(null);
    } catch (EntityNotProvidedException | EntityVersionMismatchException | EntityNotFoundException | EntityConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  public void reconfigure(String entityName, String cacheName) throws EntityAlreadyExistsException {
    EntityRef<CacheEntity, String, Object> ref = getEntityRef(entityName);
    try {
      ref.reconfigure(cacheName);
    } catch (EntityNotProvidedException | EntityNotFoundException | EntityConfigurationException e) {
      throw new AssertionError(e);
    }
  }

  private EntityRef<CacheEntity, String, Object> getEntityRef(String identifier) {
    try {
      return connection.getEntityRef(CacheEntity.class, 1, identifier);
    } catch (EntityNotProvidedException e) {
      throw new AssertionError(e);
    }
  }

}
