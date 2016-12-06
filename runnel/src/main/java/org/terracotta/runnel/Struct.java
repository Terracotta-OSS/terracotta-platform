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
import java.util.Map;

/**
 * @author Ludovic Orban
 */
public class Struct {

  private final StructField root;

  public Struct(StructField root) {
    this.root = root;
  }

  StructField alias(String name, int index) {
    return root.alias(name, index);
  }

  void init() {
    root.init();
  }

  /**
   * Create a non-thread safe encoder allowing encoding according to the present structure.
   * Note: this method is thread-safe.
   * @return the encoder.
   */
  public StructEncoder<Void> encoder() {
    root.checkFullyInitialized();
    return new StructEncoder<Void>(root);
  }

  /**
   * Create a non-thread safe decoder allowing decoding according to the present structure.
   * Note: this method is thread-safe.
   * @param byteBuffer the byte buffer containing the data to be decoded.
   * @return the decoder.
   */
  public StructDecoder<Void> decoder(ByteBuffer byteBuffer) {
    root.checkFullyInitialized();
    return new StructDecoder<Void>(root, new ReadBuffer(byteBuffer));
  }

  /**
   * Recursively decode a byte buffer according to the present structure and print the decoded outcome to a print stream.
   * Note: this method is thread-safe.
   * @param byteBuffer the byte buffer containing the data to be dumped.
   * @param out the print stream to print to.
   */
  public void dump(ByteBuffer byteBuffer, PrintStream out) {
    root.checkFullyInitialized();
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
