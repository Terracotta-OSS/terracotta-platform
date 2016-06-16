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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class PrimitiveCodec {

  public static byte[] encode(Object o) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    ObjectOutputStream output = new ObjectOutputStream(bytes);
    writeTo(output, o);
    output.close();
    return bytes.toByteArray();
  }

  public static Object decode(byte[] bytes) throws IOException {
    ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(bytes));
    return readFrom(input);
  }

  public static Object readFrom(DataInput inputStream) throws IOException {
    try {
      return ((ObjectInputStream) inputStream).readObject();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  public static void writeTo(DataOutput os, Object o) throws IOException {
    ((ObjectOutputStream) os).writeObject(o);
  }
}
