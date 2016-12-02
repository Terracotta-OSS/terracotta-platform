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
package org.terracotta.management.registry;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.management.model.call.ContextualReturn;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.context.ContextContainer;
import org.terracotta.management.registry.action.MyManagementProvider;
import org.terracotta.management.registry.action.MyObject;

import java.io.IOException;
import java.util.Collection;
import java.util.Scanner;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ManagementRegistryTest {

  @Test
  public void test_management_registry_exposes() throws IOException {
    ManagementRegistry registry = new DefaultManagementRegistry(new ContextContainer("cacheManagerName", "my-cm-name"));

    registry.addManagementProvider(new MyManagementProvider());

    registry.register(new MyObject("myCacheManagerName", "myCacheName1"));
    registry.register(new MyObject("myCacheManagerName", "myCacheName2"));

    Scanner scanner = new Scanner(ManagementRegistryTest.class.getResourceAsStream("/capabilities.json"), "UTF-8").useDelimiter("\\A");
    String expectedJson = scanner.next();
    scanner.close();

    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    mapper.addMixIn(CapabilityContext.class, CapabilityContextMixin.class);

    String actual = mapper.writeValueAsString(registry.getCapabilities());
    assertEquals(expectedJson, actual);

    ContextualReturn<?> cr = registry.withCapability("TheActionProvider")
        .call("incr", int.class, new Parameter(Integer.MAX_VALUE, "int"))
        .on(Context.empty()
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

  public static abstract class CapabilityContextMixin {
    @JsonIgnore
    public abstract Collection<String> getRequiredAttributeNames();
    @JsonIgnore
    public abstract Collection<CapabilityContext.Attribute> getRequiredAttributes();
  }

}
