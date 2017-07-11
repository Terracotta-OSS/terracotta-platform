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
package org.terracotta.management.registry.action;

import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.ExposedObject;
import org.terracotta.management.registry.Named;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Mathieu Carbou
 */
public class MyObject implements ExposedObject<MyObject> {

  private final String cmName;
  private final String cName;

  public MyObject(String cmName, String cName) {
    this.cmName = cmName;
    this.cName = cName;
  }

  @Exposed
  public int incr(@Named("n") int n) {
    if (n == Integer.MAX_VALUE) {
      throw new IllegalArgumentException();
    }
    return n + 1;
  }

  @Override
  public MyObject getTarget() {
    return this;
  }

  @Override
  public ClassLoader getClassLoader() {
    return MyObject.class.getClassLoader();
  }

  @Override
  public Context getContext() {
    return Context.empty().with("cacheManagerName", cmName).with("cacheName", cName);
  }

  @Override
  public Collection<? extends Descriptor> getDescriptors() {
    return Collections.emptyList();
  }

}
