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
package org.terracotta.dynamic_config.api.model;

import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * @author Mathieu Carbou
 */
public class TerracottaKit {

  private final String version;
  private final String buildID;
  private final Collection<Component> installedComponents;

  public TerracottaKit(String version, String buildID, Collection<Component> installedComponents) {
    this.version = version;
    this.buildID = buildID;
    this.installedComponents = installedComponents;
  }

  public String getVersion() {
    return version;
  }

  public String getBuildID() {
    return buildID;
  }

  public Collection<Component> getInstalledComponents() {
    return installedComponents;
  }

  @Override
  public String toString() {
    return getVersion() + ", as of " + getBuildID();
  }

  public static class Component {

    public enum Type {
      CONFIG,
      PLUGIN,
      SERVICE,
      ENTITY,
      UNKNOWN;

      public static Type from(String type) {
        for (Type t : values()) {
          if (t.name().equalsIgnoreCase(type)) {
            return t;
          }
        }
        return UNKNOWN;
      }
    }

    private final Type type;
    private final String name;
    private final String description;
    private final String version;
    private final String buildTimestamp;
    private final String buildJDK;

    public Component(Type type, String name, String description, String version, String buildTimestamp, String buildJDK) {
      this.type = requireNonNull(type);
      this.name = requireNonNull(name);
      this.description = description;
      this.version = version;
      this.buildTimestamp = buildTimestamp;
      this.buildJDK = buildJDK;
    }

    public Type getType() {
      return type;
    }

    public String getName() {
      return name;
    }

    public String getDescription() {
      return description;
    }

    public String getVersion() {
      return version;
    }

    public String getBuildTimestamp() {
      return buildTimestamp;
    }

    public String getBuildJDK() {
      return buildJDK;
    }

    @Override
    public String toString() {
      return String.format("%-35s %-15s (built on %s with JDK %s)", getName(), getVersion(), getBuildTimestamp(), getBuildJDK());
    }
  }
}
