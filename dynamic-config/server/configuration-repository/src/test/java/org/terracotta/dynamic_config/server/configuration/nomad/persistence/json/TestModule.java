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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.terracotta.dynamic_config.server.configuration.nomad.persistence.TestData;
import org.terracotta.json.Json;

/**
 * @author Mathieu Carbou
 */
public class TestModule extends SimpleModule implements Json.Module {
  private static final long serialVersionUID = 1L;

  public TestModule() {
    super(TestModule.class.getSimpleName(), new Version(1, 0, 0, null, null, null));

    setMixInAnnotation(TestData.Vegie.class, VegieMixin.class);
    setMixInAnnotation(TestData.Tomato.class, TomatoMixin.class);
    setMixInAnnotation(TestData.Pepper.class, PepperMixin.class);
  }

  public static abstract class VegieMixin<T extends TestData.CookingManual> extends TestData.Vegie<T> {

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    private T cookingManual;

    public VegieMixin(T cookingManual, String color) {
      super(cookingManual, color);
    }
  }

  public static class TomatoMixin extends TestData.Tomato {
    @JsonCreator
    public TomatoMixin(@JsonProperty("cookingManual") TestData.TomatoCooking cookingManual,
                  @JsonProperty("color") String color) {
      super(cookingManual, color);
    }
  }

  public static class PepperMixin extends TestData.Pepper {
    @JsonCreator
    public PepperMixin(@JsonProperty("cookingManual") TestData.TomatoCooking cookingManual,
                  @JsonProperty("color") String color) {
      super(cookingManual, color);
    }
  }
}
