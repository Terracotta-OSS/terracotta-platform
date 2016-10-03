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
package org.terracotta.management.service.monitoring.buffer;

import com.tc.classloader.CommonComponent;

/**
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ReadWriteBuffer<V> extends ReadOnlyBuffer<V> {

  /**
   * Put a value in the buffer, making some space by discarding some other values if necessary
   *
   * @return The value that has been removed, or null if none
   */
  V put(V value);

}
