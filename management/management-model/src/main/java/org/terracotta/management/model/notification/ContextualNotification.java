/**
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
package org.terracotta.management.model.notification;

import org.terracotta.management.model.Objects;
import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
public final class ContextualNotification implements Serializable {

  private static final long serialVersionUID = 1;

  private final String type;
  private final Context context;
  private final Map<String, String> attributes;

  public ContextualNotification(Context context, String type, Map<String, String> attributes) {
    this.context = Objects.requireNonNull(context);
    this.type = Objects.requireNonNull(type);
    this.attributes = new HashMap<String, String>(Objects.requireNonNull(attributes));
  }

  public ContextualNotification(Context context, String type) {
    this(context, type, Collections.<String, String>emptyMap());
  }

  public Context getContext() {
    return context;
  }

  public Map<String, String> getAttributes() {
    return attributes;
  }

  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    return "ContextualNotification{" +
        "type='" + type + '\'' +
        ", context=" + context +
        ", attributes=" + attributes +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    ContextualNotification that = (ContextualNotification) o;

    if (!type.equals(that.type)) return false;
    if (!context.equals(that.context)) return false;
    return attributes.equals(that.attributes);

  }

  @Override
  public int hashCode() {
    int result = type.hashCode();
    result = 31 * result + context.hashCode();
    result = 31 * result + attributes.hashCode();
    return result;
  }

}
