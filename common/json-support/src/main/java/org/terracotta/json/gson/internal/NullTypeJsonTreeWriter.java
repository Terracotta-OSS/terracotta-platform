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
package org.terracotta.json.gson.internal;

import com.google.gson.JsonElement;
import com.google.gson.internal.bind.JsonTreeWriter;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.io.Writer;

/**
 * {@link JsonWriter} correctly supporting {@link org.terracotta.json.Json#NULL}
 */
public class NullTypeJsonTreeWriter extends JsonWriter {
  private static final Writer UNWRITABLE_WRITER = new Writer() {
    @Override
    public void write(char[] buffer, int offset, int counter) {
      throw new AssertionError();
    }

    @Override
    public void flush() {
      throw new AssertionError();
    }

    @Override
    public void close() {
      throw new AssertionError();
    }
  };

  private final JsonTreeWriter delegate = new JsonTreeWriter();
  private final boolean serializeNulls;

  public NullTypeJsonTreeWriter(boolean serializeNulls) {
    super(UNWRITABLE_WRITER);
    this.serializeNulls = serializeNulls;
    delegate.setSerializeNulls(serializeNulls);
  }

  public JsonElement get() {
    return delegate.get();
  }

  @Override
  public JsonWriter beginArray() throws IOException {
    delegate.beginArray();
    return this;
  }

  @Override
  public JsonWriter endArray() throws IOException {
    delegate.endArray();
    return this;
  }

  @Override
  public JsonWriter beginObject() throws IOException {
    delegate.beginObject();
    return this;
  }

  @Override
  public JsonWriter endObject() throws IOException {
    delegate.endObject();
    return this;
  }

  @Override
  public JsonWriter name(String name) throws IOException {
    delegate.name(name);
    return this;
  }

  @Override
  public JsonWriter value(String value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter jsonValue(String value) throws IOException {
    // when a null is forced
    if ("null".equals(value)) {
      try {
        delegate.setSerializeNulls(true);
        delegate.nullValue();
      } finally {
        delegate.setSerializeNulls(serializeNulls);
      }
    } else {
      // can throw if the JsonWriter is a JsonTreeWriter
      delegate.jsonValue(value);
    }
    return this;
  }

  @Override
  public JsonWriter nullValue() throws IOException {
    delegate.nullValue();
    return this;
  }

  @Override
  public JsonWriter value(boolean value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter value(Boolean value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter value(float value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter value(double value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter value(long value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public JsonWriter value(Number value) throws IOException {
    delegate.value(value);
    return this;
  }

  @Override
  public void flush() throws IOException {
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public boolean isLenient() {
    return delegate.isLenient();
  }
}
