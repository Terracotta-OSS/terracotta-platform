/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.runnel.encoding.dataholders;

import org.terracotta.runnel.utils.WriteBuffer;

/**
 * @author Ludovic Orban
 */
public interface DataHolder {

  /**
   * Return the byte size of this data holder. Note that this value is cached
   * on first calculation. Memoized, even.
   * @param withIndex
   * @return byte size.
   */
  int size(boolean withIndex);

  void encode(WriteBuffer writeBuffer, boolean withIndex);

}
