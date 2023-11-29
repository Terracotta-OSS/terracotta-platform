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
package org.terracotta.voltron.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Class.forName;

/**
 * @author Alex Snaps
 */
public class SerializationCodec implements Codec {

  private static final Logger LOGGER = LoggerFactory.getLogger(SerializationCodec.class);

  private final Pattern shadingPattern;

  public SerializationCodec() {
    this.shadingPattern = null;
  }

  public SerializationCodec(Pattern shadingPattern) {
    this.shadingPattern = Objects.requireNonNull(shadingPattern);
  }

  @Override
  public byte[] encode(final Class<?> type, final Object value) {
    return serialize(value);
  }

  @Override
  public <T> T decode(final Class<T> type, final byte[] buffer) {
    return decode(type, buffer, 0, buffer.length);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <T> T decode(Class<T> type, byte[] buffer, int offset, int len) {
    return type.isPrimitive() ? (T) deserialize(buffer, offset, len) : type.cast(deserialize(buffer, offset, len));
  }

  @Override
  public byte[] encode(Class<?>[] types, Object[] values) {
    if (values == null) {
      values = new Object[0];
    }
    if (types.length != values.length) {
      throw new IllegalArgumentException();
    }
    return serialize(values);
  }

  @Override
  public Object[] decode(Class<?>[] types, byte[] buffer) {
    return decode(types, buffer, 0, buffer.length);
  }

  @Override
  public Object[] decode(Class<?>[] types, byte[] buffer, int offset, int len) {
    Object[] oo = (Object[]) deserialize(buffer, offset, len);
    for (int i = 0; i < oo.length; i++) {
      oo[i] = types[i].isPrimitive() ? oo[i] : types[i].cast(oo[i]);
    }
    return oo;
  }

  private byte[] serialize(Object value) {
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

  private Object deserialize(byte[] buffer, int offset, int len) {
    if (len == 0 || buffer.length == 0) {
      return null;
    }
    ByteArrayInputStream bin = new ByteArrayInputStream(buffer, offset, len);
    try {
      try (ObjectInputStream ois = new ObjectInputStream(bin) {

        @Override
        protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
          ObjectStreamClass descriptor = super.readClassDescriptor();
          if (shadingPattern == null) {
            return descriptor;
          } else {
            Matcher matcher = shadingPattern.matcher(descriptor.getName());
            if (matcher.matches()) {
              try {
                resolveClass(descriptor);
              } catch (ClassNotFoundException e) {
                Class<?> replacement = forName(matcher.group(1));
                ObjectStreamClass substitution = ObjectStreamClass.lookupAny(replacement);
                return checkForCompatibility(descriptor, substitution);
              }
            }
            return descriptor;
          }
        }
      }) {
        return ois.readObject();
      } catch (ClassNotFoundException e) {
        throw new RuntimeException(e);
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

  private static ObjectStreamClass checkForCompatibility(ObjectStreamClass descriptor, ObjectStreamClass substitution) throws InvalidClassException {
    if (descriptor.getSerialVersionUID() != substitution.getSerialVersionUID()) {
      throw new InvalidClassException(substitution.getName(),
          "substitute class incompatible: " +
              "stream classdesc serialVersionUID = " + descriptor.getSerialVersionUID() +
              ", substitute class serialVersionUID = " + substitution.getSerialVersionUID());
    } else {
      LOGGER.debug("Shading Auto-Substitution: {} -> {}", descriptor, substitution);
      return substitution;
    }
  }
}
