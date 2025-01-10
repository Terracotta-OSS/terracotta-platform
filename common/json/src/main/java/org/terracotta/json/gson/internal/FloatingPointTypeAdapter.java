/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.json.gson.internal;

import com.google.gson.TypeAdapter;
import com.google.gson.internal.LazilyParsedNumber;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.terracotta.json.gson.Adapters;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;

public abstract class FloatingPointTypeAdapter<T extends Number> extends TypeAdapter<T> {

  private final boolean plain;

  public FloatingPointTypeAdapter() {
    this(false);
  }

  public FloatingPointTypeAdapter(boolean plain) {
    this.plain = plain;
  }

  @Override
  public T read(JsonReader in) throws IOException {
    return convert(Adapters.JACKSON_LIKE_NUMBER_STRATEGY.readNumber(in));
  }

  @Override
  public String toString() {
    return FloatingPointTypeAdapter.class.getName() + ":" + ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0].getTypeName() + (plain ? ":plain" : "");
  }

  protected abstract T convert(Number n);

  @SuppressWarnings("UnpredictableBigDecimalConstructorCall")
  @Override
  public void write(JsonWriter out, Number value) throws IOException {
    if (value instanceof Byte
        || value instanceof Short
        || value instanceof Integer
        || value instanceof Long
        || value instanceof BigInteger
        || value instanceof LazilyParsedNumber
        || value instanceof AtomicInteger
        || value instanceof AtomicLong
        || value instanceof LongAccumulator
        || value instanceof LongAdder
    ) {
      out.value(value);
    } else {
      // we have a decimal - first check special numbers
      String string = value.toString();
      if (string.equals("-Infinity") || string.equals("Infinity") || string.equals("NaN")) {
        out.value(string);
      } else if (plain) {
        // several hacks here:
        // - new BigDecimal is used to keep the number as-is with an unlimited precision, and avoid a string conversion followed by a parsing
        // - toPlainString() avoids exponent notation
        // - Using a LazilyParsedNumber allows Gson to write the number as-is in its string representation obtained from toPlainString()
        BigDecimal bd = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.doubleValue());
        out.value(new LazilyParsedNumber(bd.toPlainString()));
      } else {
        // normal mode to write decimals with exponent notation
        out.value(value);
      }
    }
  }
}
