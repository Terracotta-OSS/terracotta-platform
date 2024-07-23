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
package org.terracotta.persistence.sanskrit.json;

import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;
import org.terracotta.persistence.sanskrit.MapSanskritVisitor;
import org.terracotta.persistence.sanskrit.TestData;

/**
 * @author Mathieu Carbou
 */
public class TestModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.writeNull(MapSanskritVisitor.Null.class);

    config.registerSuperType(TestData.Vegie.class)
        .withSubtype(TestData.Tomato.class)
        .withSubtype(TestData.Pepper.class);

    config.registerSuperType(TestData.CookingManual.class)
        .withSubtype(TestData.TomatoCooking.class);

    config.mapSuperType(TestData.Tomato.class, (json, gson, toSkip) -> new TestData.Tomato(
        gson.fromJson(json.get("cookingManual"), TestData.TomatoCooking.class),
        json.get("color").getAsString()));

    config.mapSuperType(TestData.Pepper.class, (json, gson, toSkip) -> new TestData.Pepper(
        gson.fromJson(json.get("cookingManual"), TestData.TomatoCooking.class),
        json.get("color").getAsString()));
  }
}
