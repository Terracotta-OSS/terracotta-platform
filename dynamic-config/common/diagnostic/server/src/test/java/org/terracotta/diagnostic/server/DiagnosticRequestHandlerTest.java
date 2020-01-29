/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.diagnostic.server;

import org.junit.Test;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
public class DiagnosticRequestHandlerTest {

  private final JsonDiagnosticCodec codec = new JsonDiagnosticCodec();
  private final DiagnosticRequestHandler handler = DiagnosticRequestHandler.withCodec(codec);

  @Test
  public void test_getServiceInterface() {
    handler.add(MyService.class, new MyService() {});
    assertThat(
        () -> handler.hasServiceInterface(null),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(handler.hasServiceInterface("foo"), is(false));
    assertThat(handler.hasServiceInterface(MyService.class.getName()), is(true));
  }

  @Test
  public void test_add() {
    assertThat(
        () -> handler.add(null, new MyService() {}),
        is(throwing(instanceOf(NullPointerException.class))));
    assertThat(
        () -> handler.add(MyService.class, null),
        is(throwing(instanceOf(NullPointerException.class))));

    DiagnosticServiceDescriptor<MyService> descriptor = handler.add(MyService.class, new MyService() {});
    assertThat(descriptor, is(not(nullValue())));

    assertThat(
        () -> handler.add(MyService.class, new MyService() {}),
        is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Service org.terracotta.diagnostic.server.DiagnosticRequestHandlerTest$MyService is already registered")))));
  }

  @Test
  public void test_remove() {
    assertThat(handler.remove(MyService.class), is(nullValue()));
    DiagnosticServiceDescriptor<MyService> descriptor = handler.add(MyService.class, new MyService() {});
    assertThat(handler.remove(MyService.class), is(sameInstance(descriptor)));
  }

  @Test
  public void test_findService() {
    assertThat(handler.findService(MyService.class).isPresent(), is(false));

    DiagnosticServiceDescriptor<MyService> descriptor = handler.add(MyService.class, new MyService() {});
    assertThat(handler.findService(MyService.class).isPresent(), is(true));
    assertThat(handler.findService(MyService.class).get(), is(sameInstance(descriptor)));

    handler.add(MyService2.class, new MyService2() {});
    assertThat(handler.findService(MyService.class).isPresent(), is(true));
    assertThat(handler.findService(MyService.class).get(), is(sameInstance(descriptor)));
  }

  @Test
  public void test_withCodec() {
    assertThat(
        () -> DiagnosticRequestHandler.withCodec(null),
        is(throwing(instanceOf(NullPointerException.class))));
  }

  @Test
  public void test_request() {
    assertThat(
        () -> handler.request(null),
        is(throwing(instanceOf(NullPointerException.class))));

    DiagnosticRequest request = new DiagnosticRequest(MyService.class, "hello", "you");
    String res = handler.request(handler.getCodec().serialize(request));
    assertThat(res, is(equalTo(MESSAGE_UNKNOWN_COMMAND)));

    handler.add(MyService.class, new MyService() {});
    DiagnosticResponse<?> response = handler.getCodec().deserialize(handler.request(handler.getCodec().serialize(request)), DiagnosticResponse.class);
    assertThat(response.getBody(), is(equalTo("Hello you!")));
    assertThat(response.hasError(), is(false));
  }

  public interface MyService {
    default String hello(String name) { return "Hello " + name + "!";}
  }

  public interface MyService2 {
  }
}
