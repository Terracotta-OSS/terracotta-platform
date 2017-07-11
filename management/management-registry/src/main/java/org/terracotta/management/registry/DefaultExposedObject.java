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
package org.terracotta.management.registry;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
public class DefaultExposedObject<T> implements ExposedObject<T> {
  
  private final Context context;
  private final T o;

  public DefaultExposedObject(T o, Context context) {
    this.context = Objects.requireNonNull(context);
    this.o = Objects.requireNonNull(o);
  }

  public DefaultExposedObject(T o) {
    this(o, Context.empty());
  }

  @Override
  public T getTarget() {
    return o;
  }

  @Override
  public ClassLoader getClassLoader() {
    return o.getClass().getClassLoader();
  }

  @Override
  public Context getContext() {
    return context;
  }

  @Override
  public Collection<? extends Descriptor> getDescriptors() {
    return Collections.emptyList();
  }
}
