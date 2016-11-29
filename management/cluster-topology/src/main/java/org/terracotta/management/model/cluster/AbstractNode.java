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
package org.terracotta.management.model.cluster;

import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.Contextual;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractNode<P extends Contextual> implements Node {

  private static final long serialVersionUID = 1;

  private P parent;
  private final String id;

  public AbstractNode(String id) {
    this.id = Objects.requireNonNull(id);
  }

  final void detach() {
    parent = null;
  }

  final void setParent(P parent) {
    this.parent = parent;
  }

  final P getParent() {
    return parent;
  }

  @Override
  public final String getId() {
    return id;
  }

  public Map<String, Object> toMap() {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("id", getId());
    return map;
  }

  @Override
  public void setContext(Context context) {
    // nothing: we do not replace the context of a topology
  }

  @Override
  public Context getContext() {
    return (parent == null ? Context.empty() : parent.getContext())
        .with(getContextKey(), getId());
  }

  abstract String getContextKey();

  @Override
  public final List<? extends Node> getNodePath() {
    if (parent == null || !(parent instanceof Node)) {
      return Collections.singletonList(this);
    }
    List<? extends Node> parentNodes = ((Node) parent).getNodePath();
    List<Node> path = new ArrayList<>(parentNodes.size() + 1);
    path.addAll(parentNodes);
    path.add(this);
    return path;
  }

  @Override
  public final String getStringPath() {
    List<? extends Node> nodes = getNodePath();
    if (nodes.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder(nodes.get(0).getId());
    for (int i = 1; i < nodes.size(); i++) {
      sb.append("/").append(nodes.get(i));
    }
    return sb.toString();
  }

  @Override
  public String toString() {
    return getId();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    AbstractNode<?> that = (AbstractNode<?>) o;
    return id != null ? id.equals(that.id) : that.id == null;
  }

  @Override
  public int hashCode() {
    return id != null ? id.hashCode() : 0;
  }
}
