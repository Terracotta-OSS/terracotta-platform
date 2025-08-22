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

import com.google.gson.JsonElement;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class SortKeysTypeAdapter<T> extends TypeAdapter<T> {
  private final TypeAdapter<T> delegate;
  private final TypeAdapter<JsonElement> jsonElementTypeAdapter;
  private final boolean serializeNulls;

  public SortKeysTypeAdapter(TypeAdapter<T> delegate, TypeAdapter<JsonElement> jsonElementTypeAdapter, boolean serializeNulls) {
    this.delegate = delegate;
    this.jsonElementTypeAdapter = jsonElementTypeAdapter;
    this.serializeNulls = serializeNulls;
  }

  @Override
  public String toString() {
    return getClass().getName();
  }

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    final NullTypeJsonTreeWriter writer = new NullTypeJsonTreeWriter(serializeNulls);
    writer.setSerializeNulls(serializeNulls);
    delegate.write(writer, value);
    final JsonElement element = Utils.sort(writer.get());
    final boolean backup = out.getSerializeNulls();
    try {
      out.setSerializeNulls(true);
      jsonElementTypeAdapter.write(out, element);
    } finally {
      out.setSerializeNulls(backup);
    }
  }

  @Override
  public T read(JsonReader in) throws IOException {
    return delegate.read(in);
  }
}
