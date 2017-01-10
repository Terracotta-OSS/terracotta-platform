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
package org.terracotta.management.registry.action;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.terracotta.management.model.call.Parameter;
import org.terracotta.management.model.capabilities.context.CapabilityContext;
import org.terracotta.management.model.capabilities.descriptors.CallDescriptor;
import org.terracotta.management.model.capabilities.descriptors.Descriptor;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.registry.ManagementProvider;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Mathieu Carbou
 */
@RunWith(JUnit4.class)
public class ActionProviderTest {

  ManagementProvider<MyObject> managementProvider = new MyManagementProvider();

  @Test
  public void testName() throws Exception {
    assertThat(managementProvider.getCapabilityName(), equalTo("TheActionProvider"));
  }

  @Test
  public void testDescriptors() throws Exception {
    managementProvider.register(new MyObject("myCacheManagerName", "myCacheName1"));
    managementProvider.register(new MyObject("myCacheManagerName", "myCacheName2"));

    Collection<? extends Descriptor> descriptors = managementProvider.getDescriptors();
    assertThat(descriptors.size(), is(1));
    assertThat(descriptors.iterator().next(), is(instanceOf(CallDescriptor.class)));
    assertThat((CallDescriptor) descriptors.iterator().next(), equalTo(
        new CallDescriptor("incr", "int", Collections.singletonList(new CallDescriptor.Parameter("n", "int")))
    ));
  }

  @Test
  public void testCapabilityContext() throws Exception {
    managementProvider.register(new MyObject("myCacheManagerName", "myCacheName1"));
    managementProvider.register(new MyObject("myCacheManagerName", "myCacheName2"));

    CapabilityContext capabilityContext = managementProvider.getCapabilityContext();

    assertThat(capabilityContext.getAttributes().size(), is(2));

    Iterator<CapabilityContext.Attribute> iterator = capabilityContext.getAttributes().iterator();
    CapabilityContext.Attribute next = iterator.next();
    assertThat(next.getName(), equalTo("cacheManagerName"));
    assertThat(next.isRequired(), is(true));
    next = iterator.next();
    assertThat(next.getName(), equalTo("cacheName"));
    assertThat(next.isRequired(), is(true));
  }

  @Test
  public void testCollectStatistics() throws Exception {
    try {
      managementProvider.collectStatistics(null, null);
      fail("expected UnsupportedOperationException");
    } catch (UnsupportedOperationException uoe) {
      // expected
    }
  }

  @Test
  public void testCallAction() throws Exception {
    managementProvider.register(new MyObject("cache-manager-0", "cache-0"));

    Context context = Context.empty()
        .with("cacheManagerName", "cache-manager-0")
        .with("cacheName", "cache-0");

    int n = managementProvider.callAction(context, "incr", int.class, new Parameter(1, "int"));

    assertThat(n, equalTo(2));
  }

  @Test
  public void testCallAction_bad_context() throws Exception {
    managementProvider.register(new MyObject("cache-manager-0", "cache-0"));

    Context context = Context.empty()
        .with("cacheManagerName", "cache-manager-0")
        .with("cacheName", "cache-1");

    try {
      managementProvider.callAction(context, "int", int.class, new Parameter(1, "int"));
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  @Test
  public void testCallAction_bad_method() throws Exception {
    managementProvider.register(new MyObject("cache-manager-0", "cache-0"));

    Context context = Context.empty()
        .with("cacheManagerName", "cache-manager-0")
        .with("cacheName", "cache-0");

    try {
      managementProvider.callAction(context, "clearer", Void.class);
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

  @Test
  public void testCallAction_noSuchMethod() throws Exception {
    managementProvider.register(new MyObject("cache-manager-0", "cache-0"));

    Context context = Context.empty()
        .with("cacheManagerName", "cache-manager-1")
        .with("cacheName", "cache-0");

    try {
      managementProvider.callAction(context, "int", int.class, new Parameter(1, "long"));
      fail("expected IllegalArgumentException");
    } catch (IllegalArgumentException iae) {
      // expected
    }
  }

}
