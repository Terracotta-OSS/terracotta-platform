/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import com.fasterxml.jackson.core.ObjectCodec;
import org.junit.Test;
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.json.Json;

import java.io.Closeable;
import java.io.Serializable;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticServiceDescriptorTest {

  @Test
  public void test_ctor_params_validation() {
    // param validation
    assertThat(
        () -> new DiagnosticServiceDescriptor<>(null, Json.copyObjectMapper()),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> new DiagnosticServiceDescriptor<>(Serializable.class, null),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> new DiagnosticServiceDescriptor<>(ObjectCodec.class, Json.copyObjectMapper()),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Not an interface: " + ObjectCodec.class.getName())))));
  }

  @Test
  public void test_matches() {
    DiagnosticServiceDescriptor<Serializable> descriptor = new DiagnosticServiceDescriptor<>(Serializable.class, Json.copyObjectMapper());
    assertThat(() -> descriptor.matches(null), is(throwing(instanceOf(NullPointerException.class))));
    assertThat(descriptor.matches(Serializable.class), is(true));
    assertThat(descriptor.matches(Closeable.class), is(false));
  }

  @Test
  public void test_mustBeExposed() {
    DiagnosticServiceDescriptor<Serializable> descriptor1 = new DiagnosticServiceDescriptor<>(Serializable.class, Json.copyObjectMapper());
    assertThat(descriptor1.discoverMBeanName().isPresent(), is(false));

    DiagnosticServiceDescriptor<MyService> descriptor2 = new DiagnosticServiceDescriptor<>(MyService.class, new MyServiceImpl());
    assertThat(descriptor2.discoverMBeanName().isPresent(), is(true));
  }

  @Test
  public void test_invoke() {
    DiagnosticServiceDescriptor<MyService> descriptor = new DiagnosticServiceDescriptor<>(MyService.class, new MyService() {});
    // OK
    {
      DiagnosticResponse<?> response = descriptor.invoke("bar").get();
      assertThat(response.hasError(), is(false));
      assertThat(response.getBody(), is(nullValue()));
    }
    // wrong method name
    {
      assertThat(descriptor.invoke("baz").isPresent(), is(false));
    }
    // overloading
    {
      assertThat(
          () -> descriptor.invoke("foo"),
          is(throwing(instanceOf(AssertionError.class)).andMessage(is(equalTo("Method overloading not yet supported: " + MyService.class.getName())))));
    }
    // invoke with wrong args
    {
      DiagnosticResponse<?> response = descriptor.invoke("withArgs", 1).get();
      assertThat(response.getBody(), is(nullValue()));
      assertThat(response.getError().get(), containsString(IllegalArgumentException.class.getName() + ": argument type mismatch"));
    }
    // invoke error
    {
      DiagnosticResponse<?> response = descriptor.invoke("fail").get();
      assertThat(response.getBody(), is(nullValue()));
      assertThat(response.getError().get(), containsString(IllegalArgumentException.class.getName() + ": failed"));
    }
  }

  public interface MyService {
    default void foo() {}

    default void foo(String s) {}

    default void bar() {}

    default void withArgs(String arg) {}

    default void fail() { throw new IllegalArgumentException("failed");}
  }

  @Expose("MyService")
  public static class MyServiceImpl implements MyService {}
}