/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is OffHeap Resource.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
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
}
