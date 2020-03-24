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
package org.terracotta.management.model.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class ContextContainer implements Serializable {

  private static final long serialVersionUID = 1;

  private final String name;
  private final String value;
  private final Collection<ContextContainer> subContexts;

  public ContextContainer(String name, String value) {
    this(name, value, Collections.<ContextContainer>emptyList());
  }

  public ContextContainer(String name, String value, ContextContainer... subContexts) {
    this(name, value, Arrays.asList(subContexts));
  }

  public ContextContainer(String name, String value, Collection<ContextContainer> subContexts) {
    this.name = Objects.requireNonNull(name);
    this.value = Objects.requireNonNull(value);
    this.subContexts = Objects.requireNonNull(subContexts);
  }

  public String getName() {
    return name;
  }

  public String getValue() {
    return value;
  }

  public Collection<ContextContainer> getSubContexts() {
    return subContexts;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("ContextContainer{");
    sb.append("name='").append(name).append('\'');
    sb.append(", value='").append(value).append('\'');
    sb.append(", subContexts=").append(subContexts);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextContainer that = (ContextContainer) o;

    if (!name.equals(that.name)) return false;
    if (!value.equals(that.value)) return false;
    return subContexts.equals(that.subContexts);

  }

  @Override
  public int hashCode() {
    int result = name.hashCode();
    result = 31 * result + value.hashCode();
    result = 31 * result + subContexts.hashCode();
    return result;
  }

}
