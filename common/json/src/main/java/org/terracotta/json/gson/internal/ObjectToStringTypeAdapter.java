/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.function.Function;

public class ObjectToStringTypeAdapter<T> extends TypeAdapter<T> {
  private final Function<T, String> stringifier;
  private final Function<String, T> reverse;

  public ObjectToStringTypeAdapter(Function<T, String> stringifier, Function<String, T> reverse) {
    this.stringifier = stringifier;
    this.reverse = reverse;
  }

  @Override
  public String toString() {
    return getClass().getName();
  }

  @Override
  public void write(JsonWriter out, T value) throws IOException {
    out.value(stringifier.apply(value));
  }

  @Override
  public T read(JsonReader in) throws IOException {
    return reverse.apply(in.nextString());
  }
}
