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
