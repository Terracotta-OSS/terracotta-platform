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
import org.terracotta.runnel.decoding.fields.ArrayField;
import org.terracotta.runnel.decoding.fields.Field;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.decoding.fields.ValueField;
import org.terracotta.runnel.encoding.StructEncoder;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.VLQ;

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

  public void dump(ByteBuffer byteBuffer, PrintStream out) {
    Map<Integer, Field> fieldsByInteger = root.getMetadata().buildFieldsByIndexMap();

    ReadBuffer readBuffer = new ReadBuffer(byteBuffer);
    int totalSize = readBuffer.getVlqInt();
    readBuffer = readBuffer.limit(totalSize);

    while (!readBuffer.limitReached()) {
      int index = readBuffer.getVlqInt();
      out.print("index: "); out.print(index);
      Field field = fieldsByInteger.get(index);
      dumpField(field, readBuffer, out, 0);
      out.print("\n");
    }
  }

  private void dumpField(Field field, ReadBuffer parentBuffer, PrintStream out, int depth) {
    int fieldSize = parentBuffer.getVlqInt();
    out.print(" size: "); out.print(fieldSize);
    ReadBuffer readBuffer = parentBuffer.limit(fieldSize);

    if (field instanceof ArrayField) {
      out.print(" type: "); out.print(field.getClass().getSimpleName());

      int length = readBuffer.getVlqInt();
      out.print(" length: "); out.print(length);

      ArrayField arrayField = (ArrayField) field;
      Field subField = arrayField.subField();

      for (int i = 0; i < length; i++) {
        out.print("\n  "); for (int j = 0; j < depth; j++) out.print("  ");
        dumpField(subField, readBuffer, out, depth + 1);
      }
    } else if (field instanceof StructField) {
      out.print(" type: "); out.print(field.getClass().getSimpleName());
      out.print(" name: "); out.print(field.name());

      StructField structField = (StructField) field;
      Map<Integer, Field> fieldsByInteger = structField.getMetadata().buildFieldsByIndexMap();
      while (!readBuffer.limitReached()) {
        out.print("\n  "); for (int j = 0; j < depth; j++) out.print("  ");
        int index = readBuffer.getVlqInt();
        out.print(" index: "); out.print(index);
        Field subField = fieldsByInteger.get(index);
        dumpField(subField, readBuffer, out, depth + 1);
      }
    } else if (field != null) {
      ValueField valueField = (ValueField) field;
      out.print(" type: "); out.print(field.getClass().getSimpleName());
      out.print(" name: "); out.print(field.name());
      parentBuffer.rewind(VLQ.encodedSize(fieldSize));
      Object decoded = valueField.decode(readBuffer);
      out.print(" decoded: ["); out.print(decoded); out.print("]");
    } else {
      out.print(" type: ???");
      readBuffer.skipAll();
    }
  }

}
