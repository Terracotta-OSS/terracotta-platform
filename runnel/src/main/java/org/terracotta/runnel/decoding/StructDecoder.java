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
package org.terracotta.runnel.decoding;

import org.terracotta.runnel.decoding.fields.BoolField;
import org.terracotta.runnel.decoding.fields.ByteBufferField;
import org.terracotta.runnel.decoding.fields.CharField;
import org.terracotta.runnel.decoding.fields.EnumField;
import org.terracotta.runnel.decoding.fields.FloatingPoint64Field;
import org.terracotta.runnel.decoding.fields.Int32Field;
import org.terracotta.runnel.decoding.fields.Int64Field;
import org.terracotta.runnel.decoding.fields.StringField;
import org.terracotta.runnel.decoding.fields.StructField;
import org.terracotta.runnel.metadata.FieldDecoder;
import org.terracotta.runnel.utils.MissingMandatoryFieldException;
import org.terracotta.runnel.utils.ReadBuffer;
import org.terracotta.runnel.utils.RunnelDecodingException;

import java.nio.ByteBuffer;
import java.util.Optional;

/**
 * A decoder allows decoding structured data described by a {@link org.terracotta.runnel.Struct}.
 * Note: Instances of this class are not thread-safe.
 */
public class StructDecoder<P> implements PrimitiveDecodingSupport {

  private final FieldDecoder fieldDecoder;
  private final ReadBuffer readBuffer;
  private final P parent;

  public StructDecoder(StructField structField, ReadBuffer readBuffer) throws RunnelDecodingException {
    this(structField, readBuffer, null);
  }

  public StructDecoder(StructField structField, ReadBuffer readBuffer, P parent) throws RunnelDecodingException {
    this.parent = parent;
    int size = readBuffer.getVlqInt();
    this.readBuffer = readBuffer.limit(size);
    this.fieldDecoder = structField.getMetadata().fieldDecoder(this.readBuffer);
  }

  @Override
  public Optional<Boolean> optionalBool(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, BoolField.class));
  }

  @Override
  public boolean mandatoryBool(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalBool);
  }

  @Override
  public Optional<Character> optionalChr(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, CharField.class));
  }

  @Override
  public char mandatoryChr(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalChr);
  }

  @Override
  public Optional<Integer> optionalInt32(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, Int32Field.class));
  }

  @Override
  public int mandatoryInt32(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalInt32);
  }

  @Override
  public <E> Enm<E> optionalEnm(String name) throws RunnelDecodingException {
    Enm<E> enm = (Enm<E>) fieldDecoder.decodeValue(name, (Class) EnumField.class);
    if (enm == null) {
      return new Enm<>(name);
    }
    return enm;
  }

  @Override
  public <E> Enm<E> mandatoryEnm(String name) throws RunnelDecodingException {
    Enm<E> result = optionalEnm(name);

    if (!result.isFound()) {
      throw new MissingMandatoryFieldException(name);
    }

    if (!result.isValid()) {
      throw new RunnelDecodingException("Enum not valid: " + name + " value: " + result.raw());
    }

    return result;
  }

  @Override
  public Optional<Long> optionalInt64(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, Int64Field.class));
  }

  @Override
  public long mandatoryInt64(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalInt64);
  }

  @Override
  public Optional<Double> optionalFp64(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, FloatingPoint64Field.class));
  }

  @Override
  public double mandatoryFp64(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalFp64);
  }

  @Override
  public Optional<String> optionalString(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, StringField.class));
  }

  @Override
  public String mandatoryString(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalString);
  }

  @Override
  public Optional<ByteBuffer> optionalByteBuffer(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValue(name, ByteBufferField.class));
  }

  @Override
  public ByteBuffer mandatoryByteBuffer(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalByteBuffer);
  }


  public Optional<ArrayDecoder<Integer, StructDecoder<P>>> optionalInt32s(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, Int32Field.class, this));
  }

  public ArrayDecoder<Integer, StructDecoder<P>> mandatoryInt32s(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalInt32s);
  }

  public Optional<ArrayDecoder<Boolean, StructDecoder<P>>> optionalBools(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, BoolField.class, this));
  }

  public ArrayDecoder<Boolean, StructDecoder<P>> mandatoryBools(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalBools);
  }

  public Optional<ArrayDecoder<Character, StructDecoder<P>>> optionalChrs(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, CharField.class, this));
  }

  public ArrayDecoder<Character, StructDecoder<P>> mandatoryChrs(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalChrs);
  }

  public Optional<ArrayDecoder<Long, StructDecoder<P>>> optionalInt64s(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, Int64Field.class, this));
  }

  public ArrayDecoder<Long, StructDecoder<P>> mandatoryInt64s(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalInt64s);
  }

  public Optional<ArrayDecoder<Double, StructDecoder<P>>> optionalFp64s(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, FloatingPoint64Field.class, this));
  }

  public ArrayDecoder<Double, StructDecoder<P>> mandatoryFp64s(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalFp64s);
  }

  public Optional<ArrayDecoder<String, StructDecoder<P>>> optionalStrings(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeValueArray(name, StringField.class, this));
  }

  public ArrayDecoder<String, StructDecoder<P>> mandatoryStrings(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalStrings);
  }

  public Optional<StructDecoder<StructDecoder<P>>> optionalStruct(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeStruct(name, this));
  }

  public StructDecoder<StructDecoder<P>> mandatoryStruct(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalStruct);
  }

  public Optional<StructArrayDecoder<StructDecoder<P>>> optionalStructs(String name) throws RunnelDecodingException {
    return Optional.ofNullable(fieldDecoder.decodeStructArray(name, this));
  }

  public StructArrayDecoder<StructDecoder<P>> mandatoryStructs(String name) throws RunnelDecodingException {
    return mandatory(name, this::optionalStructs);
  }

  public P end() {
    if (parent == null) {
      throw new IllegalStateException("Cannot end root decoder");
    }

    readBuffer.skipAll();

    return parent;
  }

  private <T> T mandatory(String name, FieldFetcher<T> fieldFetcher) throws RunnelDecodingException {
    return fieldFetcher.fetch(name).orElseThrow(() -> new MissingMandatoryFieldException(name));
  }

  @FunctionalInterface
  private interface FieldFetcher<T> {
    Optional<T> fetch(String name) throws RunnelDecodingException;
  }
}
