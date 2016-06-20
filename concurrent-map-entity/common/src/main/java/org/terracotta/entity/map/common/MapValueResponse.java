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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;


public class MapValueResponse implements MapResponse {
  private final Object value;

  public MapValueResponse(Object value) {
    this.value = value;
  }

  public Object getValue() {
    return this.value;
  }

  @Override
  public Type responseType() {
    return Type.MAP_VALUE;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, this.value);
  }

  static MapValueResponse readFrom(DataInput input) throws IOException {
    return new MapValueResponse(PrimitiveCodec.readFrom(input));
  }
}
