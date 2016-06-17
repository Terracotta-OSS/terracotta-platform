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

package org.terracotta.entity.map;

import org.terracotta.entity.map.common.ValueWrapper;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * ValueCodecFactory
 */
public class ValueCodecFactory {
  static <T> ValueCodec<T> getCodecForClass(Class<T> clazz) {
    if (!Serializable.class.isAssignableFrom(clazz)) {
      throw new IllegalArgumentException("The provided type is invalid as it is not Serializable " + clazz);
    }
    if (Integer.class.equals(clazz) || Long.class.equals(clazz)
        || Float.class.equals(clazz) || Double.class.equals(clazz)
        || Byte.class.equals(clazz) || Character.class.equals(clazz)
        || clazz.isPrimitive() || String.class.equals(clazz)) {
      return new IdentityCodec<T>();
    } else {
      return new SerializationWrapperCodec<T>();
    }
  }

  private static class IdentityCodec<T> implements ValueCodec<T> {
    @Override
    public Object encode(T input) {
      return input;
    }

    @Override
    public T decode(Object input) {
      return (T) input;
    }
  }

  private static class SerializationWrapperCodec<T> implements ValueCodec<T> {
    @Override
    public Object encode(T input) {
      if (input == null) {
        return null;
      }
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try {
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        try {
          oos.writeObject(input);
        } catch(IOException e) {
          throw new RuntimeException("Object cannot be serialized", e);
        } finally {
          oos.close();
        }
      } catch(IOException e) {
        // ignore
      }
      return new ValueWrapper(input.hashCode(), baos.toByteArray());
    }

    @Override
    public T decode(Object input) {
      if (input == null) {
        return null;
      }
      ValueWrapper data = (ValueWrapper) input;
      ByteArrayInputStream bais = new ByteArrayInputStream(data.getValue());
      try {
        ObjectInputStream ois = new ObjectInputStream(bais);
        try {
          return (T) ois.readObject();
        } catch (ClassNotFoundException e) {
          throw new RuntimeException("Could not load class", e);
        } finally {
          ois.close();
        }
      } catch(IOException e) {
        // ignore
      }
      throw new AssertionError("Cannot reach here!");
    }
  }
}
