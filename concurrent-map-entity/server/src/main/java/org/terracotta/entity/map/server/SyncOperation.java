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

package org.terracotta.entity.map.server;

import org.terracotta.entity.map.common.MapOperation;

import java.io.DataOutput;
import java.io.IOException;
import java.util.Map;


class SyncOperation implements MapOperation {
  private final Map<Object, Object> objects;

  public SyncOperation(Map<Object, Object> objects) {
    this.objects = objects;
  }

  @Override
  public Type operationType() {
    return Type.SYNC_OP;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {

  }

  public Map<Object, Object> getObjectMap() {
    return objects;
  }
}
