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
package org.terracotta.runnel;

import org.terracotta.runnel.decoding.StructDecoder;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.encoding.StructEncoder;
import org.terracotta.runnel.utils.ReadBuffer;

import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Struct {
  private final StructField root;

  public Struct(StructField root) {
    this.root = root;
  }

  List<? extends Field> getRootSubFields() {
    return root.subFields();
  }

  public StructEncoder encoder() {
    return new StructEncoder(root);
  }

  public StructDecoder decoder(ByteBuffer byteBuffer) {
    return new StructDecoder(root, new ReadBuffer(byteBuffer));
  }

  public StructDecoder fromDecoder(StructDecoder otherDecoder) {
    return new StructDecoder(root, otherDecoder);
  }

  public void dump(ByteBuffer byteBuffer, PrintStream out) {
    Map<Integer, Field> fieldsByInteger = root.getMetadata().buildFieldsByIndexMap();

    ReadBuffer readBuffer = new ReadBuffer(byteBuffer);
    int totalSize = readBuffer.getVlqInt();
    readBuffer = readBuffer.limit(totalSize);

    while (!readBuffer.limitReached()) {
      int index = readBuffer.getVlqInt();
      out.append("index: ").append(Integer.toString(index));
      Field field = fieldsByInteger.get(index);
      if (field != null) {
        field.dump(readBuffer, out, 0);
      } else {
        int fieldSize = readBuffer.getVlqInt();
        out.append(" size: ").append(Integer.toString(fieldSize));
        ReadBuffer fieldReadBuffer = readBuffer.limit(fieldSize);

        out.append(" type: ???");
        fieldReadBuffer.skipAll();
      }
      out.append("\n");
    }
  }

}
