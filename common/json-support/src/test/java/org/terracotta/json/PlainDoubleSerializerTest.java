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
package org.terracotta.json;

import com.google.gson.internal.bind.TypeAdapters;
import org.junit.Test;
import org.terracotta.json.gson.Adapters;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import static java.util.Collections.singletonMap;
import static org.junit.Assert.assertEquals;

public class PlainDoubleSerializerTest {

  Json json = new DefaultJsonFactory().withModule(new MyGsonModule()).create();

  @Test
  public void serialize_value() {
    assertEquals("1", json.toString(1.0));
    assertEquals("100000000000", json.toString(100000000000.0));
    assertEquals("179769313486231570814527423731704356798070567525844996598917476803157260780028538760589558632766878171540458953514382464234321326889464182768467546703537516986049910576551282076245490090389328944075868508455133942304583236903222948165808559332123348274797826204144723168738177180919299881250404026184124858368", json.toString(Double.MAX_VALUE));
    assertEquals("0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000004940656458412465441765687928682213723650598026143247644255856825006755072702087518652998363616359923797965646954457177309266567103559397963987747960107818781263007131903114045278458171678489821036887186360569987307230500063874091535649843873124733972731696151400317153853980741262385655911710266585566867681870395603106249319452715914924553293054565444011274801297099995419319894090804165633245247571478690147267801593552386115501348035264934720193790268107107491703332226844753335720832431936092382893458368060106011506169809753078342277318329247904982524730776375927247874656084778203734469699533647017972677717585125660551199131504891101451037862738167250955837389733598993664809941164205702637090279242767544565229087538682506419718265533447265625", json.toString(Double.MIN_VALUE));
    assertEquals("{\"n\":1.100000000000000088817841970012523233890533447265625}", json.toString(singletonMap("n", 1.1)));
  }

  @Test
  public void serialize_NaN() {
    assertEquals("\"NaN\"", json.toString(Double.NaN));
  }

  @Test
  public void serialize_Pos_Infinite() {
    assertEquals("\"Infinity\"", json.toString(Double.POSITIVE_INFINITY));
  }

  @Test
  public void serialize_Neg_Infinite() {
    assertEquals("\"-Infinity\"", json.toString(Double.NEGATIVE_INFINITY));
  }

  @Json.Module.Overrides(TerracottaJsonModule.class)
  private static class MyGsonModule implements GsonModule {
    @Override
    public void configure(GsonConfig config) {
      config.registerTypeAdapterFactory(TypeAdapters.newFactory(double.class, Double.class, Adapters.PLAIN_DOUBLES));
    }
  }
}
