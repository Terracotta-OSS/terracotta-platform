/**
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
package org.terracotta.voltron.proxy;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * @author Alex Snaps
 */
public class SerializationCodec implements Codec {

  public byte[] encode(final Class<?> type, final Object value) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oout = new ObjectOutputStream(bout);
      oout.writeObject(value);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        bout.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
    return bout.toByteArray();
  }

  public byte[] encode(final Class<?>[] type, final Object[] values) {
    ByteArrayOutputStream bout = new ByteArrayOutputStream();
    try {
      ObjectOutputStream oout = new ObjectOutputStream(bout);
      oout.writeObject(values);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        bout.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
    return bout.toByteArray();
  }

  public Object decode(final byte[] buffer, final Class<?> type) {
    ByteArrayInputStream bin = new ByteArrayInputStream(buffer);
    try {
      ObjectInputStream ois = new ObjectInputStream(bin);
      try {
        return ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      } finally {
        ois.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        bin.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }  }

  public Object[] decode(final byte[] buffer, final Class<?>[] type) {
    ByteArrayInputStream bin = new ByteArrayInputStream(buffer);
    try {
      ObjectInputStream ois = new ObjectInputStream(bin);
      try {
        return (Object[]) ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
      } finally {
        ois.close();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    } finally {
      try {
        bin.close();
      } catch (IOException e) {
        throw new AssertionError(e);
      }
    }
  }
}
