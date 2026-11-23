/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
package org.terracotta.dynamic_config.testing.gson;

import org.terracotta.json.gson.GsonConfig;
import org.terracotta.json.gson.GsonModule;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test fixture discovered through META-INF/services by {@link java.util.ServiceLoader}.
 * Tests can check {@link #CONFIGURED} to verify that this module was applied to a Json factory.
 */
public class TestGsonModule implements GsonModule {

  public static final AtomicBoolean CONFIGURED = new AtomicBoolean();

  @Override
  public void configure(GsonConfig config) {
    CONFIGURED.set(true);
  }
}
