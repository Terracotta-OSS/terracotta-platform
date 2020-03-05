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
package org.terracotta.diagnostic.server;

import com.tc.management.TerracottaManagement;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import javax.management.InstanceNotFoundException;
import javax.management.MBeanInfo;
import java.io.Closeable;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.reset;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DiagnosticServicesTest {

  @Spy MyService1Impl service1;
  @Spy MyServiceImpl service2;

  @After
  public void tearDown() {
    DiagnosticServices.clear();
    reset(service1, service2);
  }

  @Test
  public void test_register() {
    assertThat(
        () -> DiagnosticServices.register(null, service1),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> DiagnosticServices.register(MyService1.class, null),
        is(throwing(instanceOf(NullPointerException.class))));

    assertThat(DiagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(
        () -> DiagnosticServices.register(MyService1.class, service1),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Service org.terracotta.diagnostic.server.DiagnosticServicesTest$MyService1 is already registered")))));
    assertThat(DiagnosticServices.register(MyService2.class, service2), is(instanceOf(DiagnosticServicesRegistration.class)));
  }

  @Test
  public void test_unregister() {
    assertThat(
        () -> DiagnosticServices.unregister(null),
        is(throwing(instanceOf(NullPointerException.class))));

    // does not crash when unregister inexisting service
    DiagnosticServices.unregister(Closeable.class);

    assertThat(DiagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(DiagnosticServices.findService(MyService1.class).isPresent(), is(true));
    DiagnosticServices.unregister(MyService1.class);
    assertThat(DiagnosticServices.findService(MyService1.class).isPresent(), is(false));
  }

  @Test
  public void test_findService() {
    assertThat(
        () -> DiagnosticServices.findService(null),
        is(throwing(instanceOf(NullPointerException.class))));

    assertThat(DiagnosticServices.findService(MyService1.class).isPresent(), is(false));

    assertThat(DiagnosticServices.register(MyService1.class, service1), is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(DiagnosticServices.findService(MyService1.class).isPresent(), is(true));

    assertThat(DiagnosticServices.findService(MyService2.class).isPresent(), is(false));
  }

  @Test
  public void test_expose() throws Exception {
    DiagnosticServicesRegistration<MyService1> s1Registration = DiagnosticServices.register(MyService1.class, service1);
    assertThat(s1Registration, is(instanceOf(DiagnosticServicesRegistration.class)));
    assertThat(DiagnosticServices.register(MyService2.class, service2), is(instanceOf(DiagnosticServicesRegistration.class)));

    MBeanInfo s2 = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(TerracottaManagement.createObjectName(null, "s2", TerracottaManagement.MBeanDomain.PUBLIC));
    assertThat(s2, is(not(nullValue())));

    assertThat(
        () -> ManagementFactory.getPlatformMBeanServer().getMBeanInfo(TerracottaManagement.createObjectName(null, "s1", TerracottaManagement.MBeanDomain.PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=s1")))));

    s1Registration.registerMBean("s1");

    MBeanInfo s1 = ManagementFactory.getPlatformMBeanServer().getMBeanInfo(TerracottaManagement.createObjectName(null, "s1", TerracottaManagement.MBeanDomain.PUBLIC));
    assertThat(s1, is(not(nullValue())));
  }

  @Test
  public void test_unexpose() throws Exception {
    assertThat(DiagnosticServices.register(MyService2.class, service2), is(instanceOf(DiagnosticServicesRegistration.class)));

    ManagementFactory.getPlatformMBeanServer().getMBeanInfo(TerracottaManagement.createObjectName(null, "s2", TerracottaManagement.MBeanDomain.PUBLIC));

    DiagnosticServices.unregister(MyService2.class);

    assertThat(
        () -> ManagementFactory.getPlatformMBeanServer().getMBeanInfo(TerracottaManagement.createObjectName(null, "s2", TerracottaManagement.MBeanDomain.PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=s2")))));
  }

  @Test
  public void test_async_support() {
    AtomicInteger i = new AtomicInteger();
    DiagnosticServices.onService(MyService1.class).thenRun(i::incrementAndGet);
    assertThat(i.get(), is(equalTo(0)));

    DiagnosticServices.register(MyService1.class, service1);
    assertThat(i.get(), is(equalTo(1)));

    DiagnosticServices.onService(MyService1.class).thenRun(i::incrementAndGet);
    assertThat(i.get(), is(equalTo(2)));
  }

  public interface MyService1 {
    String say1(String word);
  }

  public static class MyService1Impl implements MyService1 {
    @Override
    public String say1(String word) {
      return "1. Hello " + word;
    }
  }

  public interface MyService2 {
    String say2(String word);
  }

  @Expose("s2")
  public static class MyServiceImpl implements MyService2 {
    @Override
    public String say2(String word) {
      return "2. Hello " + word;
    }
  }

}