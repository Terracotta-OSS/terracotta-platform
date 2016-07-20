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

package org.terracotta.state;

import com.tc.classloader.CommonComponent;

import java.util.Map;

/**
 * Repository to hold state of entities.
 * This class is not thread safe, since it's always expected
 * that state changes only during lifecycle of entity under maintenance.
 */
@CommonComponent
public interface EntityStateRepository {

  <K, V> Map<K, V> getOrCreateState(String name, Class<K> kClass, Class<V> vClass);

  void destroyState(String name);
}
