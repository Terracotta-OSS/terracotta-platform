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
package org.terracotta.management.entity.sample.server;

import org.terracotta.entity.ServiceConfiguration;

import java.util.Map;

/**
 * @author Mathieu Carbou
 */
@SuppressWarnings("rawtypes")
public class MapConfiguration implements ServiceConfiguration<Map> {

  private final String name;

  public MapConfiguration(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public Class<Map> getServiceType() {
    return Map.class;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("MapConfiguration{");
    sb.append("name='").append(name).append('\'');
    sb.append('}');
    return sb.toString();
  }
}
