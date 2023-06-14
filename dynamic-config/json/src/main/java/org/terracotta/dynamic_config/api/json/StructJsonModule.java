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
package org.terracotta.dynamic_config.api.json;

import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.common.struct.Version;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import java.io.IOException;

/**
 * @author Mathieu Carbou
 */
public class StructJsonModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.objectToString(Version.class, Version::valueOf);
    config.registerTypeHierarchyAdapter(new TypeToken<Measure<?>>() {}, new TypeAdapter<Measure<?>>() {
      @Override
      public void write(JsonWriter out, Measure<?> value) throws IOException {
        out.beginObject();
        out.name("quantity").value(value.getExactQuantity());
        out.name("type");
        if (value.getUnit() instanceof TimeUnit) {
          out.value("TIME");
        } else if (value.getUnit() instanceof MemoryUnit) {
          out.value("MEMORY");
        } else {
          throw new IllegalArgumentException("Unsupported type: " + value.getUnit().getClass());
        }
        out.name("unit").value(value.getUnit().name());
        out.endObject();
      }

      @Override
      public Measure<?> read(JsonReader in) throws IOException {
        long quantity = 0;
        String unit = null;
        String type = null;
        in.beginObject();
        while (in.peek() != JsonToken.END_OBJECT) {
          switch (in.nextName()) {
            case "quantity":
              quantity = in.nextLong();
              break;
            case "unit":
              unit = in.nextString();
              break;
            case "type":
              type = in.nextString();
              break;
            default:
              in.skipValue();
          }
        }
        in.endObject();
        if (unit != null && type != null) {
          switch (type) {
            case "TIME":
              return Measure.of(quantity, TimeUnit.valueOf(unit));
            case "MEMORY":
              return Measure.of(quantity, MemoryUnit.valueOf(unit));
            default:
              // ignore
          }
        }
        throw new IllegalArgumentException("Invalid measure: quantity=" + quantity + ", unit=" + unit + ", type=" + type);
      }
    });
  }
}
