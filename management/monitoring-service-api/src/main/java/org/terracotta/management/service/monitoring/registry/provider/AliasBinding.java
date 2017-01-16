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
package org.terracotta.management.service.monitoring.registry.provider;

import com.tc.classloader.CommonComponent;

import java.util.Objects;

@CommonComponent
public class AliasBinding {

  private final String alias;
  private final Object value;

  public AliasBinding(String alias, Object value) {
    this.alias = Objects.requireNonNull(alias);
    this.value = Objects.requireNonNull(value);
  }

  public String getAlias() {
    return alias;
  }

  public Object getValue() {
    return value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AliasBinding that = (AliasBinding) o;
    return alias.equals(that.alias);
  }

  @Override
  public int hashCode() {
    return alias.hashCode();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
    sb.append("alias=").append(alias);
    sb.append(", value=").append(value);
    sb.append('}');
    return sb.toString();
  }

}
