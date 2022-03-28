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
package org.terracotta.management.model.capabilities.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.context.Context;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Objects;

/**
 * @author Ludovic Orban
 * @author Mathieu Carbou
 */
public final class CapabilityContext implements Serializable {

  private static final Logger logger = LoggerFactory.getLogger(CapabilityContext.class);

  private static final long serialVersionUID = 1;

  private final Collection<Attribute> attributes;

  public CapabilityContext(Attribute... attributes) {
    this(Arrays.asList(attributes));
  }

  public CapabilityContext(Collection<Attribute> attributes) {
    this.attributes = new ArrayList<Attribute>(Objects.requireNonNull(attributes));
  }

  public Collection<Attribute> getAttributes() {
    return attributes;
  }

  public Collection<String> getRequiredAttributeNames() {
    Collection<String> names = new LinkedHashSet<String>();
    for (Attribute attribute : this.attributes) {
      if (attribute.isRequired()) {
        names.add(attribute.getName());
      }
    }
    return names;
  }

  /**
   * Check if the context contains at least all required attributes
   */
  public boolean isValid(Context context) {
    if (context == null) {
      return false;
    }
    for (CapabilityContext.Attribute attribute : getRequiredAttributes()) {
      if (context.get(attribute.getName()) == null) {
        logger.debug("Required attribute: {} not found in context: {}", attribute.getName(), context);
        return false;
      }
    }
    return true;
  }

  public Collection<Attribute> getRequiredAttributes() {
    Collection<Attribute> attributes = new ArrayList<Attribute>(this.attributes.size());
    for (Attribute attribute : this.attributes) {
      if (attribute.isRequired()) {
        attributes.add(attribute);
      }
    }
    return attributes;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("CapabilityContext{");
    sb.append("attributes=").append(attributes);
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    CapabilityContext that = (CapabilityContext) o;

    return attributes.equals(that.attributes);

  }

  @Override
  public int hashCode() {
    return attributes.hashCode();
  }

  public static final class Attribute implements Serializable {

    private static final long serialVersionUID = 1;

    private final String name;
    private final boolean required;

    public Attribute(String name, boolean required) {
      this.name = Objects.requireNonNull(name);
      this.required = required;
    }

    public String getName() {
      return name;
    }

    public boolean isRequired() {
      return required;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      Attribute attribute = (Attribute) o;

      if (required != attribute.required) return false;
      return name.equals(attribute.name);

    }

    @Override
    public int hashCode() {
      int result = name.hashCode();
      result = 31 * result + (required ? 1 : 0);
      return result;
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder("Attribute{");
      sb.append("name='").append(name).append('\'');
      sb.append(", required=").append(required);
      sb.append('}');
      return sb.toString();
    }
  }

}
