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
package org.terracotta.diagnostic.common.json;

import org.terracotta.diagnostic.common.JsonDiagnosticCodecTest;
import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import java.io.Closeable;
import java.time.Duration;

/**
 * @author Mathieu Carbou
 */
public class TestModule implements GsonModule {
  @Override
  public void configure(GsonConfig config) {
    config.registerSuperType(JsonDiagnosticCodecTest.Vegie.class, "@class", Class::getName).withSubtypes(JsonDiagnosticCodecTest.Tomato.class, JsonDiagnosticCodecTest.Pepper.class);
    config.registerSuperType(JsonDiagnosticCodecTest.CookingManual.class, "@class", Class::getName).withSubtypes(JsonDiagnosticCodecTest.CookingManual.class, JsonDiagnosticCodecTest.TomatoCooking.class);

    config.allowClassLoading(Closeable.class, JsonDiagnosticCodecTest.Tomato.class, JsonDiagnosticCodecTest.Pepper.class, Duration.class);
  }
}
