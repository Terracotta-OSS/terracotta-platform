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
package org.terracotta.json;

import com.google.gson.internal.bind.TypeAdapters;
import org.junit.Test;
import org.terracotta.json.gson.Adapters;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class PlainFloatSerializerTest {

  Json json = new DefaultJsonFactory().withModule(new MyGsonModule()).create();

  @Test
  public void serialize_value() {
    assertEquals("1", json.toString(1.0f));
    assertEquals("1000000000", json.toString(1000000000.0f));
    assertEquals("340282346638528859811704183484516925440", json.toString(Float.MAX_VALUE));
    assertEquals("0.00000000000000000000000000000000000000000000140129846432481707092372958328991613128026194187651577175706828388979108268586060148663818836212158203125", json.toString(Float.MIN_VALUE));
    assertEquals("{\"n\":1.10000002384185791015625}", json.toString(singletonMap("n", 1.1f)));
  }

  @Test
  public void serialize_NaN() {
    assertEquals("\"NaN\"", json.toString(Float.NaN));
  }

  @Test
  public void serialize_Pos_Infinite() {
    assertEquals("\"Infinity\"", json.toString(Float.POSITIVE_INFINITY));
  }

  @Test
  public void serialize_Neg_Infinite() {
    assertEquals("\"-Infinity\"", json.toString(Float.NEGATIVE_INFINITY));
  }

  @Json.Module.Overrides(TerracottaJsonModule.class)
  private static class MyGsonModule implements GsonModule {
    @Override
    public void configure(GsonConfig config) {
      config.registerTypeAdapterFactory(TypeAdapters.newFactory(float.class, Float.class, Adapters.PLAIN_FLOATS));
    }
  }
}
