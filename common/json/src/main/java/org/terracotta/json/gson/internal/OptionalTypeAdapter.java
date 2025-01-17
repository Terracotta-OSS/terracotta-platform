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

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class OptionalTypeAdapter<V> extends TypeAdapter<Optional<V>> {
  private final Gson gson;
  private final TypeToken<V> type;
  private final TypeAdapter<V> delegate;

  public OptionalTypeAdapter(Gson gson, TypeToken<V> declaredType, TypeAdapter<V> delegate) {
    this.gson = requireNonNull(gson);
    this.type = declaredType; // nullable
    this.delegate = delegate; // nullable
  }

  @Override
  public String toString() {
    return getClass().getName() + ":Optional<" + type + ">";
  }

  @SuppressWarnings("unchecked")
  @Override
  public void write(JsonWriter out, Optional<V> o) throws IOException {
    if (o.isPresent()) {
      final V value = o.get();
      final Class<V> runtimeType = (Class<V>) value.getClass();
      final TypeAdapter<V> runtimeTypeAdapter = gson.getAdapter(runtimeType);
      runtimeTypeAdapter.write(out, value);
    } else {
      out.nullValue();
    }
  }

  @Override
  public Optional<V> read(JsonReader in) throws IOException {
    if (delegate == null) {
      throw new IllegalStateException("Unable to read optional from json at: " + in.getPath());
    }
    return Optional.ofNullable(delegate.read(in));
  }
}
