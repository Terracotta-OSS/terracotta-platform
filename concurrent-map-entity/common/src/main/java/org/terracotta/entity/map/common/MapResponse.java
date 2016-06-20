/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.entity.map.common;

import org.terracotta.entity.EntityResponse;

import java.io.DataOutput;
import java.io.IOException;


public interface MapResponse extends EntityResponse {
  enum Type {
    // "Primitive" response values - only zero or one instance.
    NULL,
    BOOLEAN,
    SIZE,
    // "Complex" response values - collections.
    MAP_VALUE,
    KEY_SET,
    VALUE_COLLECTION,
    ENTRY_SET,
  }

  Type responseType();

  void writeTo(DataOutput output) throws IOException;
}
