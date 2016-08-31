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
package org.terracotta.management.entity.management;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.cluster.ClientIdentifier;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public abstract class ManagementEvent implements Serializable {

  private static final long serialVersionUID = 1;

  private final String id;
  private final ClientIdentifier from;

  protected ManagementEvent(String id, ClientIdentifier from) {
    this.from = Objects.requireNonNull(from);
    this.id = Objects.requireNonNull(id);
  }

  public final String getId() {
    return id;
  }

  public final ClientIdentifier getFrom() {
    return from;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    ManagementEvent that = (ManagementEvent) o;
    if (!id.equals(that.id)) return false;
    return from.equals(that.from);
  }

  @Override
  public int hashCode() {
    int result = id.hashCode();
    result = 31 * result + from.hashCode();
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ManagementEvent{");
    sb.append("id='").append(id).append('\'');
    sb.append(", from=").append(from);
    sb.append('}');
    return sb.toString();
  }
}
