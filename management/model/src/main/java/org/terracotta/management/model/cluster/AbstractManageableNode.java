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
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;

import java.util.Objects;
import java.util.Optional;

/**
 * Be careful to implement equals/hashcode correctly (see {@link Client and {@link ServerEntity}}
 *
 * @author Mathieu Carbou
 */
public abstract class AbstractManageableNode<P extends Contextual> extends AbstractNode<P> {

  private static final long serialVersionUID = 1;

  private volatile ManagementRegistry managementRegistry;

  public AbstractManageableNode(String id) {
    super(id);
  }

  public final Optional<ManagementRegistry> getManagementRegistry() {
    return Optional.ofNullable(managementRegistry);
  }

  public final void setManagementRegistry(ManagementRegistry managementRegistry) {
    this.managementRegistry = managementRegistry;
  }

  public final boolean isManageable() {
    return managementRegistry != null;
  }

  @Override
  public Context getContext() {
    return super.getContext().with(getManagementRegistry().map(ManagementRegistry::getContext).orElse(Context.empty()));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    AbstractManageableNode<?> that = (AbstractManageableNode<?>) o;

    return Objects.equals(managementRegistry, that.managementRegistry);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (managementRegistry != null ? managementRegistry.hashCode() : 0);
    return result;
  }
}
