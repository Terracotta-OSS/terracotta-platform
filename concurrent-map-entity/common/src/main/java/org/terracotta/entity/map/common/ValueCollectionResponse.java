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
import java.util.Collection;


public class ValueCollectionResponse implements MapResponse {
  private final Collection<Object> values;

  public ValueCollectionResponse(Collection<Object> values) {
    this.values = values;
  }

  public Collection<Object> getValues() {
    return this.values;
  }

  @Override
  public Type responseType() {
    return Type.VALUE_COLLECTION;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, this.values);
  }

  @SuppressWarnings("unchecked")
  static ValueCollectionResponse readFrom(DataInput input) throws IOException {
    return new ValueCollectionResponse((Collection<Object>) PrimitiveCodec.readFrom(input));
  }
}
