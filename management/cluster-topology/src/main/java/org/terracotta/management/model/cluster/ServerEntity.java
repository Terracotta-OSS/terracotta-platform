/**
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
import java.util.Objects;
import java.util.Optional;

/**
 * @author Mathieu Carbou
 */
public final class ServerEntity extends AbstractNode<Server> implements Serializable {

  private static final long serialVersionUID = 1;

  public static final String KEY = "entityId";
  public static final String TYPE_KEY = "entityType";
  public static final String NAME_KEY = "entityName";

  private final String type; // type (service, client entity, server entity, etc)
  private final String name; // type (entity type name)
  private ManagementRegistry managementRegistry;

  // matches management registry config, or entity id, or service type
  private ServerEntity(String id, String name, String type) {
    super(id);
    this.type = Objects.requireNonNull(type);
    this.name = Objects.requireNonNull(name);
  }

  public Optional<ManagementRegistry> getManagementRegistry() {
    return Optional.ofNullable(managementRegistry);
  }

  public ServerEntity setManagementRegistry(ManagementRegistry managementRegistry) {
    this.managementRegistry = managementRegistry;
    return this;
  }

  public String getType() {
    return type;
  }

  public boolean isType(String type) {
    return this.type.equals(type);
  }

  public String getName() {
    return name;
  }

  @Override
  public Context getContext() {
    return super.getContext().with(NAME_KEY, name).with(TYPE_KEY, type);
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
    return this.name.equals(name) && this.type.equals(type);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    ServerEntity that = (ServerEntity) o;

    if (!type.equals(that.type)) return false;
    if (!name.equals(that.name)) return false;
    return managementRegistry != null ? managementRegistry.equals(that.managementRegistry) : that.managementRegistry == null;

  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + type.hashCode();
    result = 31 * result + name.hashCode();
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
    return new ServerEntity(key(serverEntityName, type), serverEntityName, type);
  }

  public static String key(String serverEntityName, String type) {
    return serverEntityName + ":" + type;
  }

}
