/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.offheapresource;

import org.terracotta.entity.ServiceConfiguration;

import com.tc.classloader.CommonComponent;

/**
 * Provides identification of a server-defined off-heap resource.
 */
@CommonComponent
public final class OffHeapResourceIdentifier implements ServiceConfiguration<OffHeapResource> {

  private final String name;

  public static OffHeapResourceIdentifier identifier(String name) {
    return new OffHeapResourceIdentifier(name);
  }

  private OffHeapResourceIdentifier(String name) {
    if (name == null) {
      throw new NullPointerException("Name cannot be null");
    } else {
      this.name = name;
    }
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof OffHeapResourceIdentifier) && name.equals(((OffHeapResourceIdentifier) obj).name);
  }

  public String getName() {
    return name;
  }

  @Override
  public Class<OffHeapResource> getServiceType() {
    return OffHeapResource.class;
  }


  @Override
  public String toString() {
    return "OffHeapResourceIdentifier{" +
        "name='" + name + '\'' +
        '}';
  }
}
