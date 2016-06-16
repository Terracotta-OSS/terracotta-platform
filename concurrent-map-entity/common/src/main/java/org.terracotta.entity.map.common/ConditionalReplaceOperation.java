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

public class ConditionalReplaceOperation implements MapOperation {
  private final Object key;
  private final Object oldValue;
  private final Object newValue;

  public ConditionalReplaceOperation(Object key, Object oldValue, Object newValue) {
    this.key = key;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  public Object getKey() {
    return key;
  }

  public Object getOldValue() {
    return oldValue;
  }

  public Object getNewValue() {
    return newValue;
  }

  @Override
  public Type operationType() {
    return Type.CONDITIONAL_REPLACE;
  }

  @Override
  public void writeTo(DataOutput output) throws IOException {
    PrimitiveCodec.writeTo(output, key);
    PrimitiveCodec.writeTo(output, oldValue);
    PrimitiveCodec.writeTo(output, newValue);
  }

  static ConditionalReplaceOperation readFrom(DataInput dataInput) throws IOException {
    return new ConditionalReplaceOperation(PrimitiveCodec.readFrom(dataInput), PrimitiveCodec.readFrom(dataInput), PrimitiveCodec.readFrom(dataInput));
  }
}
