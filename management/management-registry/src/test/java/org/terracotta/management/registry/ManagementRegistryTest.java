/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.management.registry;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.json.DefaultJsonFactory;
import org.terracotta.json.Json;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.action.MyManagementProvider;
import org.terracotta.management.registry.action.MyObject;
import org.terracotta.management.registry.json.TestModule;

import java.io.File;
import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementRegistryTest {

  @Test
  public void test_management_registry_exposes() throws URISyntaxException {
    ManagementRegistry registry = new DefaultManagementRegistry(new ContextContainer("cacheManagerName", "my-cm-name"));

    registry.addManagementProvider(new MyManagementProvider());

    registry.register(new MyObject("myCacheManagerName", "myCacheName1"));
    registry.register(new MyObject("myCacheManagerName", "myCacheName2"));

    Json json = new DefaultJsonFactory().withModule(new TestModule()).create();
    Object expected = json.parse(new File(ManagementRegistryTest.class.getResource("/capabilities.json").toURI()));
    Object actual = json.map(registry.getCapabilities());
    assertEquals(expected, actual);

    ContextualReturn<?> cr = registry.withCapability("TheActionProvider")
        .call("incr", int.class, new Parameter(Integer.MAX_VALUE, "int"))
        .on(Context.empty()
            .with("instanceId", "instance-0")
            .with("cacheManagerName", "myCacheManagerName")
            .with("cacheName", "myCacheName1"))
        .build()
        .execute()
        .getSingleResult();

    try {
      cr.getValue();
      fail();
    } catch (ExecutionException e) {
      assertEquals(IllegalArgumentException.class, e.getCause().getClass());
    }
  }
}
