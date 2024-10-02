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
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PathTypeAdapter extends TypeAdapter<Path> {
  @Override
  public String toString() {
    return PathTypeAdapter.class.getName();
  }

  @Override
  public void write(JsonWriter out, Path value) throws IOException {
    out.beginArray();
    for (Path segment : value) {
      out.value(segment.toString());
    }
    out.endArray();
  }

  @Override
  public Path read(JsonReader in) throws IOException {
    if (in.peek() == JsonToken.STRING) {
      // backward compat with V1
      return Paths.get(in.nextString());
    } else {
      // new parsing, using arrays
      in.beginArray();
      String first = in.nextString();
      List<String> more = new ArrayList<>();
      while (in.hasNext()) {
        more.add(in.nextString());
      }
      in.endArray();
      return Paths.get(first, more.toArray(new String[0]));
    }
  }
}
