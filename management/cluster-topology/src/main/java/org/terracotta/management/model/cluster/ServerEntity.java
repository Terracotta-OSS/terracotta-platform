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
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.Map;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public final class ServerEntity extends AbstractNode<Server> implements Serializable {

  private static final long serialVersionUID = 2;

  public static final String KEY = "entityId";
  public static final String TYPE_KEY = "entityType";
  public static final String NAME_KEY = "entityName";

  private final ServerEntityIdentifier identifier;
  private ManagementRegistry managementRegistry;

  // matches management registry config, or entity id, or service type
  private ServerEntity(ServerEntityIdentifier identifier) {
    super(identifier.getId());
    this.identifier = identifier;
  }

  public Optional<ManagementRegistry> getManagementRegistry() {
    return Optional.ofNullable(managementRegistry);
  }

  public ServerEntity setManagementRegistry(ManagementRegistry managementRegistry) {
    this.managementRegistry = managementRegistry;
    return this;
  }

  public ServerEntityIdentifier getServerEntityIdentifier() {
    return identifier;
  }

  public String getType() {
    return identifier.getType();
  }

  public boolean isType(String type) {
    return getType().equals(type);
  }

  public String getName() {
    return identifier.getName();
  }

  @Override
  public Context getContext() {
    return super.getContext()
        .with(NAME_KEY, getName())
        .with(TYPE_KEY, getType());
  }

  @Override
  public void remove() {
    Server parent = getParent();
    if (parent != null) {
      parent.removeServerEntity(getId());
    }
  }

  public Server getServer() {
    return getParent();
  }

  @Override
  String getContextKey() {
    return KEY;
  }

  public boolean is(String name, String type) {
    return this.getName().equals(name) && this.getType().equals(type);
  }

  public boolean is(ServerEntityIdentifier identifier) {
    return this.identifier.equals(identifier);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ServerEntity that = (ServerEntity) o;

    if (!identifier.equals(that.identifier)) return false;
    return managementRegistry != null ? managementRegistry.equals(that.managementRegistry) : that.managementRegistry == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + identifier.hashCode();
    result = 31 * result + (managementRegistry != null ? managementRegistry.hashCode() : 0);
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("type", getType());
    map.put("name", getName());
    map.put("managementRegistry", managementRegistry == null ? null : managementRegistry.toMap());
    return map;
  }

  public static ServerEntity create(String serverEntityName, String type) {
    return create(ServerEntityIdentifier.create(serverEntityName, type));
  }

  public static ServerEntity create(ServerEntityIdentifier serverEntityIdentifier) {
    return new ServerEntity(serverEntityIdentifier);
  }

  public static String key(String serverEntityName, String type) {
    return serverEntityName + ":" + type;
  }

}
