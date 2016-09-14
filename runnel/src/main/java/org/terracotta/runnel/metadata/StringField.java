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
package org.terracotta.runnel.metadata;

import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

/**
 * @author Ludovic Orban
 */
public class StringField extends AbstractField {
  public StringField(String name, int index) {
    super(name, index);
  }

  @Override
  public Object decode(ReadBuffer readBuffer) {
    int len = readBuffer.getVlqInt();
    return readBuffer.getString(len);
  }

  @Override
  public int skip(ReadBuffer readBuffer) {
    int len = readBuffer.getVlqInt();
    readBuffer.skip(len);
    return len + VLQ.encodedSize(len);
  }
}
