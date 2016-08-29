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
package org.terracotta.management.entity.tms;

import java.io.Serializable;

/**
 * @author Mathieu Carbou
 */
public final class TmsAgentConfig implements Serializable {

  private static final long serialVersionUID = 1;

  // name must be hardcoded because it reference a class name in client package and is used on server-side
  public static final String ENTITY_TYPE = "org.terracotta.management.entity.tms.client.TmsAgentEntity";

  private String stripeName;
  private int maximumUnreadMutations = 1024 * 1024;
  private int maximumUnreadNotifications = 1024 * 1024;
  private int maximumUnreadStatistics = 1024 * 1024;

  public int getMaximumUnreadMutations() {
    return maximumUnreadMutations;
  }

  public TmsAgentConfig setMaximumUnreadMutations(int maximumUnreadMutations) {
    this.maximumUnreadMutations = maximumUnreadMutations;
    return this;
  }

  public String getStripeName() {
    return stripeName;
  }

  public TmsAgentConfig setStripeName(String stripeName) {
    this.stripeName = stripeName;
    return this;
  }

  public int getMaximumUnreadNotifications() {
    return maximumUnreadNotifications;
  }

  public TmsAgentConfig setMaximumUnreadNotifications(int maximumUnreadNotifications) {
    this.maximumUnreadNotifications = maximumUnreadNotifications;
    return this;
  }

  public int getMaximumUnreadStatistics() {
    return maximumUnreadStatistics;
  }

  public TmsAgentConfig setMaximumUnreadStatistics(int maximumUnreadStatistics) {
    this.maximumUnreadStatistics = maximumUnreadStatistics;
    return this;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TmsAgentConfig that = (TmsAgentConfig) o;

    if (maximumUnreadMutations != that.maximumUnreadMutations) return false;
    if (maximumUnreadNotifications != that.maximumUnreadNotifications) return false;
    if (maximumUnreadStatistics != that.maximumUnreadStatistics) return false;
    return stripeName != null ? stripeName.equals(that.stripeName) : that.stripeName == null;

  }

  @Override
  public int hashCode() {
    int result = stripeName != null ? stripeName.hashCode() : 0;
    result = 31 * result + maximumUnreadMutations;
    result = 31 * result + maximumUnreadNotifications;
    result = 31 * result + maximumUnreadStatistics;
    return result;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("TmsAgentConfig{");
    sb.append("stripeName='").append(stripeName).append('\'');
    sb.append(", maximumUnreadMutations=").append(maximumUnreadMutations);
    sb.append(", maximumUnreadNotifications=").append(maximumUnreadNotifications);
    sb.append(", maximumUnreadStatistics=").append(maximumUnreadStatistics);
    sb.append('}');
    return sb.toString();
  }
  
}
