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
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Collection;

public class AllowedClassTypeAdapter extends TypeAdapter<Class<?>> implements UnsafeClassSupport {
  private final Collection<String> allowed;
  private final TypeAdapter<Class<?>> delegate;
  private final TypeAdapter<JsonElement> jsonElementAdapter;
  private final ClassLoader classLoader;

  public AllowedClassTypeAdapter(Gson gson, TypeAdapter<Class<?>> delegate, ClassLoader classLoader, Collection<String> allowed) {
    this.allowed = allowed;
    this.delegate = delegate;
    this.jsonElementAdapter = gson.getAdapter(JsonElement.class);
    this.classLoader = classLoader;
  }

  @Override
  public String toString() {
    return getClass().getName() + ":" + allowed;
  }

  @Override
  public boolean isAllowed(String cname) {
    return allowed.contains(cname);
  }

  @Override
  public Class<?> load(String cname) {
    if (!isAllowed(cname)) {
      throw new IllegalArgumentException("Unauthorized to load: " + cname);
    }
    try {
      return Class.forName(cname, true, classLoader);
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    }
  }

  @Override
  public void write(JsonWriter out, Class<?> value) throws IOException {
    if (value == null) {
      out.nullValue();
    } else {
      final String cname = value.getName();
      if (allowed.contains(cname)) {
        out.value(cname);
      } else {
        delegate.write(out, value);
      }
    }
  }

  @Override
  public Class<?> read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.NULL) {
      in.nextNull();
      return null;
    } else {
      final JsonElement element = jsonElementAdapter.read(in);
      if (element.isJsonPrimitive()) {
        final JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (primitive.isString()) {
          String cname = primitive.getAsString();
          if (isAllowed(cname)) {
            return load(cname);
          }
        }
      }
      return delegate.fromJsonTree(element);
    }
  }
}
