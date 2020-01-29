/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import org.junit.Test;

import javax.management.InstanceNotFoundException;

import static com.tc.management.TerracottaManagement.MBeanDomain.PUBLIC;
import static com.tc.management.TerracottaManagement.createObjectName;
import static java.lang.management.ManagementFactory.getPlatformMBeanServer;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServicesRegistrationTest {

  @Test
  public void test_close() throws Throwable {
    assertThat(DiagnosticServices.findService(MyService2.class).isPresent(), is(false));

    DiagnosticServicesRegistration<MyService2> registration = DiagnosticServices.register(MyService2.class, new MyServiceImpl());

    assertThat(DiagnosticServices.findService(MyService2.class).isPresent(), is(true));
    assertThat(getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "s2", PUBLIC)), is(not(nullValue())));

    assertThat(registration.registerMBean("AnotherName"), is(true));

    assertThat(getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "AnotherName", PUBLIC)), is(not(nullValue())));

    registration.close();

    assertThat(DiagnosticServices.findService(MyService2.class).isPresent(), is(false));
    assertThat(
        () -> getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "s2", PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=s2")))));
    assertThat(
        () -> getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "AnotherName", PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=AnotherName")))));

    // subsequent init is not failing
    DiagnosticServices.register(MyService2.class, new MyServiceImpl()).close();
  }

  @Test
  public void test_registerMBean() throws Throwable {
    DiagnosticServicesRegistration<MyService2> registration = DiagnosticServices.register(MyService2.class, new MyServiceImpl());
    assertThat(registration.registerMBean("foo"), is(true));
    assertThat(registration.registerMBean("bar"), is(true));

    assertThat(getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "s2", PUBLIC)), is(not(nullValue())));
    assertThat(getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "foo", PUBLIC)), is(not(nullValue())));
    assertThat(getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "bar", PUBLIC)), is(not(nullValue())));

    registration.close();

    assertThat(registration.registerMBean("foo"), is(false));
    assertThat(registration.registerMBean("baz"), is(false));

    assertThat(
        () -> getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "s2", PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=s2")))));
    assertThat(
        () -> getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "foo", PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=foo")))));
    assertThat(
        () -> getPlatformMBeanServer().getMBeanInfo(createObjectName(null, "bar", PUBLIC)),
        is(throwing(instanceOf(InstanceNotFoundException.class)).andMessage(is(equalTo("org.terracotta:name=bar")))));
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