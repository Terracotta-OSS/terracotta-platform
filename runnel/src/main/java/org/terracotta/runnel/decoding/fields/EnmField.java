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

import org.terracotta.runnel.Enm;
import org.terracotta.runnel.utils.ReadBuffer;

/**
 * @author Ludovic Orban
 */
public class EnmField<E extends Enum<E>> extends AbstractField implements ValueField<E> {

  private final Enm<E> enm;

  public EnmField(String name, int index, Enm<E> enm) {
    super(name, index);
    this.enm = enm;
  }

  public Enm<E> getEnm() {
    return enm;
  }

  @Override
  public E decode(ReadBuffer readBuffer) {
    readBuffer.getVlqInt();
    int intValue = readBuffer.getVlqInt();
    return enm.toEnum(intValue);
  }

}
