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
package org.terracotta.dynamic_config.server.configuration;

import org.junit.Before;
import org.junit.Test;
import org.terracotta.dynamic_config.testing.gson.TestGsonModule;
import org.terracotta.json.Json;

import java.net.URL;
import java.net.URLClassLoader;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class DynamicConfigConfigurationProviderTest {

  @Before
  public void reset() {
    TestGsonModule.CONFIGURED.set(false);
  }

  @Test
  public void createJsonFactory_appliesDiscoveredGsonModules() {
    Json.Factory factory = new DynamicConfigConfigurationProvider().createJsonFactory(getClass().getClassLoader());
    assertNotNull(factory.create()); // building the mapper calls configure() on each module
    assertTrue(TestGsonModule.CONFIGURED.get());
  }

  @Test
  public void createJsonFactory_withoutDiscoverableGsonModules() {
    ClassLoader empty = new URLClassLoader(new URL[0], null);
    Json.Factory factory = new DynamicConfigConfigurationProvider().createJsonFactory(empty);
    assertNotNull(factory.create());
    assertFalse(TestGsonModule.CONFIGURED.get());
  }
}
