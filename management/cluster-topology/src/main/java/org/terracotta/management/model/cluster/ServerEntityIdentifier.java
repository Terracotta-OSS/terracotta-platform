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

import java.io.Serializable;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public class ServerEntityIdentifier implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String type; // the type of the entity (class type usually)
  private final String name; // the name of the entity

  private ServerEntityIdentifier(String name, String type) {
    this.type = Objects.requireNonNull(type);
    this.name = Objects.requireNonNull(name);
  }

  public String getName() {
    return name;
  }

  public String getType() {
    return type;
  }

  public String getId() {
    return name + ":" + type;
  }

  @Override
  public String toString() {
    return getId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ServerEntityIdentifier that = (ServerEntityIdentifier) o;
    return type.equals(that.type) && name.equals(that.name);
  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + name.hashCode();
    return result;
  }

  public static ServerEntityIdentifier create(String name, String type) {
    return new ServerEntityIdentifier(name, type);
  }

}
