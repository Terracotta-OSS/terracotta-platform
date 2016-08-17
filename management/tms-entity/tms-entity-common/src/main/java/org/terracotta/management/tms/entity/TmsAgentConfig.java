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
package org.terracotta.management.tms.entity;

import org.terracotta.management.model.Objects;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public final class TmsAgentConfig implements Serializable {

  private static final long serialVersionUID = 1;

  // name must be hardcoded because it reference a class name in client package and is used on server-side
  public static final String ENTITY_TYPE = "org.terracotta.management.tms.entity.client.TmsAgentEntity";

  private final String connectionName;
  private final String stripeName;

  public TmsAgentConfig(String connectionName, String stripeName) {
    this.connectionName = Objects.requireNonNull(connectionName);
    this.stripeName = Objects.requireNonNull(stripeName);
  }

  public TmsAgentConfig(String connectionName) {
    this.connectionName = Objects.requireNonNull(connectionName);
    this.stripeName = null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TmsAgentConfig that = (TmsAgentConfig) o;

    if (!connectionName.equals(that.connectionName)) return false;
    return stripeName != null ? stripeName.equals(that.stripeName) : that.stripeName == null;

  }

  @Override
  public int hashCode() {
    int result = connectionName.hashCode();
    result = 31 * result + (stripeName != null ? stripeName.hashCode() : 0);
    return result;
  }

  public String getConnectionName() {
    return connectionName;
  }

  public String getStripeName() {
    return stripeName;
  }

  public TmsAgentConfig withStripeName(String stripeName) {
    return new TmsAgentConfig(connectionName, stripeName);
  }

}
