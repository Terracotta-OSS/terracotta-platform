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
package org.terracotta.runnel.decoding.fields;

import org.terracotta.runnel.EnumMapping;
import org.terracotta.runnel.decoding.Enm;
import org.terracotta.runnel.utils.ReadBuffer;

import java.io.PrintStream;

/**
 * @author Ludovic Orban
 */
public class EnumField<E> extends AbstractValueField<Enm<E>> {

  private final EnumMapping<E> enumMapping;

  public EnumField(String name, int index, EnumMapping<E> enumMapping) {
    super(name, index);
    this.enumMapping = enumMapping;
  }

  public EnumMapping<E> getEnumMapping() {
    return enumMapping;
  }

  @Override
  public Enm<E> decode(ReadBuffer readBuffer) {
    readBuffer.getVlqInt();
    int intValue = readBuffer.getVlqInt();
    E e = enumMapping.toEnum(intValue);
    return new Enm<>(name(), intValue, e);
  }

  @Override
  public void dump(ReadBuffer readBuffer, PrintStream out, int depth) {
    out.append(" type: ").append(getClass().getSimpleName());
    out.append(" name: ").append(name());
    out.append(" decoded: [");
    readBuffer.getVlqInt();
    int intValue = readBuffer.getVlqInt();
    Object decoded = enumMapping.toEnum(intValue);
    if (decoded == null) {
      decoded = intValue;
    }
    out.append(decoded.toString()).append("]");
  }

}
