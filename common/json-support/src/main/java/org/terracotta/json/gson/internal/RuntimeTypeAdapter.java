/*
 * Copyright (C) 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.terracotta.json.gson.RuntimeTypeAdapterFactory;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

/**
 * Adaptation of {@link com.google.gson.internal.bind.ReflectiveTypeAdapterFactory.TypeAdapterRuntimeTypeWrapper}.
 * <p>
 * Type adapter that can be used to map a field without knowing its type in advance:
 * * it will be serialized by using its runtime type and deserialize using its declared type.
 *
 * @see org.terracotta.json.gson.internal.MixinTypeAdapterFactory
 * @see RuntimeTypeAdapterFactory
 */
public class RuntimeTypeAdapter<T> extends TypeAdapter<T> {
  private final Gson context;
  private final TypeAdapter<T> delegate;
  private final TypeToken<T> type;

  public RuntimeTypeAdapter(Gson gson, TypeAdapter<T> delegate, TypeToken<T> type) {
    this.context = requireNonNull(gson);
    this.delegate = requireNonNull(delegate);
    this.type = requireNonNull(type);
  }

  @Override
  public String toString() {
    return getClass().getName() + ":" + type;
  }

  @Override
  public T read(JsonReader in) throws IOException {
    return delegate.read(in);
  }

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    // Order of preference for choosing type adapters
    // First preference: a type adapter registered for the runtime type
    // Second preference: a type adapter registered for the declared type
    // Third preference: reflective type adapter for the runtime type (if it is a sub class of the declared type)
    // Fourth preference: reflective type adapter for the declared type
    TypeToken<T> runtimeType = getRuntimeTypeIfMoreSpecific(type, value);
    if (!runtimeType.equals(type)) {
      TypeAdapter<T> runtimeTypeAdapter = context.getAdapter(runtimeType);
      runtimeTypeAdapter.write(out, value);
    } else {
      delegate.write(out, value);
    }
  }

  /**
   * Finds a runtime type if it is more specific.
   */
  @SuppressWarnings("unchecked")
  private static <T> TypeToken<T> getRuntimeTypeIfMoreSpecific(TypeToken<T> typeToken, T value) {
    if (value != null && typeToken.getRawType().isAssignableFrom(value.getClass())) {
      return (TypeToken<T>) TypeToken.get(value.getClass());
    }
    return typeToken;
  }
}
