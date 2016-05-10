/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Mathieu Carbou
 */
abstract class AbstractNodeWithManageable<P extends Contextual, B> extends AbstractNode<P> implements NodeWithManageable<B> {

  // services, server entities, client entities, etc.
  private final ConcurrentMap<String, Manageable> manageables = new ConcurrentHashMap<>();

  public AbstractNodeWithManageable(String id) {
    super(id);
  }

  @Override
  public final Map<String, Manageable> getManageables() {
    return manageables;
  }

  @Override
  public final int getManageableCount() {
    return manageables.size();
  }

  @Override
  public final Stream<Manageable> manageableStream() {
    return manageables.values().stream();
  }

  @Override
  @SuppressWarnings("unchecked")
  public final B addManageable(Manageable manageable) {
    // manageabled are unique per their ID but also per their combination of (type + name)
    for (Manageable m : manageables.values()) {
      if (m.is(manageable.getType(), manageable.getName())) {
        throw new IllegalArgumentException("Duplicate manageable: type=" + manageable.getType() + ", name=" + manageable.getName());
      }
    }
    if (manageables.putIfAbsent(manageable.getId(), manageable) != null) {
      throw new IllegalArgumentException("Duplicate manageable: " + manageable.getId());
    }
    manageable.setParent(this);
    return (B) this;
  }

  @Override
  public final Optional<Manageable> getManageable(Context context) {
    return getManageable(context.get(Manageable.KEY));
  }

  @Override
  public final Optional<Manageable> getManageable(String id) {
    return id == null ? Optional.empty() : Optional.ofNullable(manageables.get(id));
  }

  @Override
  public final Optional<Manageable> getManageable(String name, String type) {
    return manageableStream().filter(manageable -> manageable.is(name, type)).findFirst();
  }

  @Override
  public final boolean hasManageable(String name, String type) {
    return getManageable(name, type).isPresent();
  }

  @Override
  public final Optional<Manageable> removeManageable(String id) {
    Optional<Manageable> manageable = getManageable(id);
    manageable.ifPresent(m -> {
      if (manageables.remove(id, m)) {
        m.detach();
      }
    });
    return manageable;
  }

  @Override
  public final Stream<Manageable> manageableStream(String type) {
    return manageableStream().filter(manageable -> manageable.isType(type));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    AbstractNodeWithManageable<?, ?> that = (AbstractNodeWithManageable<?, ?>) o;
    return manageables.equals(that.manageables);
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + manageables.hashCode();
    return result;
  }

  @Override
  public Map<String, Object> toMap() {
    Map<String, Object> map = super.toMap();
    map.put("manageables", manageableStream().sorted((o1, o2) -> o1.getId().compareTo(o2.getId())).map(Manageable::toMap).collect(Collectors.toList()));
    return map;
  }

}
