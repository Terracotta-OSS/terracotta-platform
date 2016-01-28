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
package org.terracotta.management.context;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * @author Ludovic Orban
 */
public final class ContextContainer implements Serializable {

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
    this.name = name;
    this.value = value;
    this.subContexts = subContexts;
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
}
