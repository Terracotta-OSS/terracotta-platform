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
package org.terracotta.diagnostic.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.terracotta.diagnostic.Diagnostics;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.connection.Connection;
import org.terracotta.diagnostic.common.Base64DiagnosticCodec;
import org.terracotta.diagnostic.common.DiagnosticRequest;
import org.terracotta.diagnostic.common.DiagnosticResponse;
import org.terracotta.diagnostic.common.EmptyParameterDiagnosticCodec;
import org.terracotta.diagnostic.common.JavaDiagnosticCodec;
import org.terracotta.diagnostic.common.JsonDiagnosticCodec;
import org.terracotta.json.Json;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.terracotta.common.struct.Tuple3.tuple3;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_DIAGNOSTIC_REQUEST_HANDLER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MBEAN_SERVER;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_INVALID_JMX;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_NOT_PERMITTED;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_NULL_RETURN;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_REQUEST_TIMEOUT;
import static org.terracotta.diagnostic.common.DiagnosticConstants.MESSAGE_UNKNOWN_COMMAND;
import static org.terracotta.diagnostic.model.LogicalServerState.ACTIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE;
import static org.terracotta.diagnostic.model.LogicalServerState.PASSIVE_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.START_SUSPENDED;
import static org.terracotta.diagnostic.model.LogicalServerState.UNKNOWN;
import static org.terracotta.testing.ExceptionMatcher.throwing;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DiagnosticServiceImplTest {

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock public Diagnostics diagnostics;
  @Mock public Connection connection;
  @Spy public JsonDiagnosticCodec jsonCodec;
  @Spy public JavaDiagnosticCodec javaCodec;
  @Captor public ArgumentCaptor<String> request;

  DiagnosticService service;

  @Before
  public void setUp() {
    service = DiagnosticServiceFactory.getDiagnosticService(connection, diagnostics, jsonCodec);
  }

  @Test
  public void test_close() throws IOException {
    service.close();
    verify(diagnostics, times(1)).close();
    verify(connection, times(1)).close();
  }

  @Test
  public void test_close_not_fail() throws IOException {
    doThrow(new IllegalStateException()).when(diagnostics).close();
    service.close();
    verify(diagnostics, times(1)).close();
    verify(connection, times(1)).close();

    reset(diagnostics);
    reset(connection);
    doThrow(new IllegalStateException()).when(connection).close();
    service.close();
    verify(diagnostics, times(1)).close();
    verify(connection, times(1)).close();
  }

  @Test
  public void test_isConnected() {
    Stream.of("foo", MESSAGE_NOT_PERMITTED, MESSAGE_UNKNOWN_COMMAND, MESSAGE_INVALID_JMX).forEach(ret -> {
      when(diagnostics.getState()).thenReturn(ret);
      assertThat(service.isConnected(), is(true));
    });
    Stream.of(null, MESSAGE_REQUEST_TIMEOUT).forEach(ret -> {
      when(diagnostics.getState()).thenReturn(ret);
      assertThat(service.isConnected(), is(false));
    });
    verify(diagnostics, times(6)).getState();
  }

  @Test
  public void test_general_exceptions() {
    when(diagnostics.invoke(MBEAN_SERVER, "getTCProperties")).thenReturn(MESSAGE_REQUEST_TIMEOUT);
    assertThat(
        () -> service.getTCProperties(),
        is(throwing(instanceOf(DiagnosticOperationTimeoutException.class)).andMessage(is(equalTo(MESSAGE_REQUEST_TIMEOUT)))));

    when(diagnostics.invoke(MBEAN_SERVER, "getTCProperties")).thenReturn(MESSAGE_INVALID_JMX);
    assertThat(
        () -> service.getTCProperties(),
        is(throwing(instanceOf(DiagnosticOperationExecutionException.class)).andMessage(is(equalTo(MESSAGE_INVALID_JMX)))));

    when(diagnostics.invoke(MBEAN_SERVER, "getTCProperties")).thenReturn(MESSAGE_UNKNOWN_COMMAND);
    assertThat(
        () -> service.getTCProperties(),
        is(throwing(instanceOf(DiagnosticOperationUnsupportedException.class)).andMessage(is(equalTo(MESSAGE_UNKNOWN_COMMAND)))));

    when(diagnostics.invoke(MBEAN_SERVER, "getTCProperties")).thenReturn(MESSAGE_NOT_PERMITTED);
    assertThat(
        () -> service.getTCProperties(),
        is(throwing(instanceOf(DiagnosticOperationNotAllowedException.class)).andMessage(is(equalTo(MESSAGE_NOT_PERMITTED)))));

    when(diagnostics.invoke(MBEAN_SERVER, "getTCProperties")).thenReturn(null);
    assertThat(
        () -> service.getTCProperties(),
        is(throwing(instanceOf(DiagnosticConnectionException.class)).andMessage(is(nullValue(String.class)))));
  }

  @Test
  public void test_null_return_supported() {
    when(diagnostics.invoke(MBEAN_SERVER, "getEnvironment")).thenReturn("");
    assertThat(service.getEnvironment(), is(nullValue()));
  }

  @Test
  public void test_server_termination_special_returns() {
    when(diagnostics.terminateServer()).thenReturn(null);
    assertThat(() -> service.terminateServer(), is(not(throwing())));

    when(diagnostics.forceTerminateServer()).thenReturn(null);
    assertThat(() -> service.forceTerminateServer(), is(not(throwing())));
  }

  @Test
  public void test_getLogicalServerState() {
    {
      Stream.of(MESSAGE_UNKNOWN_COMMAND, MESSAGE_INVALID_JMX)
          .flatMap(mainCall -> Stream.of(MESSAGE_UNKNOWN_COMMAND, MESSAGE_INVALID_JMX)
              .flatMap(stateCall -> Stream.of(MESSAGE_UNKNOWN_COMMAND, MESSAGE_INVALID_JMX)
                  .map(blockedCall -> tuple3(mainCall, stateCall, blockedCall))))
          .forEach(input -> {
            reset(diagnostics);
            when(diagnostics.invoke("Server", "isReconnectWindow")).thenReturn("false");
            when(diagnostics.invoke("DetailedServerState", "getDetailedServerState")).thenReturn(input.t1);
            when(diagnostics.getState()).thenReturn(input.t2);
            when(diagnostics.invoke("ConsistencyManager", "isBlocked")).thenReturn(input.t3);
            assertThat(input.toString(), service.getLogicalServerState(), is(UNKNOWN));
            verify(diagnostics, times(1)).getState();
            verify(diagnostics, times(1)).invoke("ConsistencyManager", "isBlocked");
          });
    }

    {
      Stream.of(MESSAGE_UNKNOWN_COMMAND, MESSAGE_INVALID_JMX).forEach(mainCall -> {
        reset(diagnostics);
        when(diagnostics.invoke("Server", "isReconnectWindow")).thenReturn("false");
        when(diagnostics.invoke("DetailedServerState", "getDetailedServerState")).thenReturn(mainCall);
        when(diagnostics.getState()).thenReturn("ACTIVE");
        when(diagnostics.invoke("ConsistencyManager", "isBlocked")).thenReturn("true");
        assertThat(service.getLogicalServerState(), is(ACTIVE_SUSPENDED));
        verify(diagnostics, times(1)).getState();
        verify(diagnostics, times(1)).invoke("ConsistencyManager", "isBlocked");
      });
    }

    reset(diagnostics);
    when(diagnostics.invoke("DetailedServerState", "getDetailedServerState")).thenReturn("ACTIVE_SUSPENDED");
    assertThat(service.getLogicalServerState(), equalTo(ACTIVE_SUSPENDED));
    verify(diagnostics, never()).getState();
    verify(diagnostics, never()).invoke("ConsistencyManager", "isBlocked");

    reset(diagnostics);

    when(diagnostics.invoke("DetailedServerState", "getDetailedServerState")).thenReturn(MESSAGE_INVALID_JMX);
    when(diagnostics.getState()).thenReturn("PASSIVE");

    when(diagnostics.invoke("ConsistencyManager", "isBlocked")).thenReturn(MESSAGE_INVALID_JMX);
    when(diagnostics.invoke("Server", "isReconnectWindow")).thenReturn("false");
    assertThat(service.getLogicalServerState(), equalTo(PASSIVE));

    when(diagnostics.invoke("ConsistencyManager", "isBlocked")).thenReturn("true");
    assertThat(service.getLogicalServerState(), equalTo(PASSIVE_SUSPENDED));

    when(diagnostics.getState()).thenReturn("START-STATE");
    assertThat(service.getLogicalServerState(), equalTo(START_SUSPENDED));
  }

  @Test
  public void test_getProxy() {
    // parameter checking
    {
      assertThat(
          () -> service.getProxy(null),
          is(throwing(instanceOf(NullPointerException.class))));
      assertThat(
          () -> service.getProxy(Object.class),
          is(throwing(instanceOf(IllegalArgumentException.class)).andMessage(is(equalTo("Interface expected")))));
    }

    // getServiceInterface bad returns
    {
      when(diagnostics.invokeWithArg(
          MBEAN_DIAGNOSTIC_REQUEST_HANDLER,
          "hasServiceInterface",
          FoodService.class.getName()))
          .thenReturn(MESSAGE_NULL_RETURN);
      assertThat(
          () -> service.getProxy(Closeable.class),
          is(throwing(instanceOf(DiagnosticConnectionException.class))));

      when(diagnostics.invokeWithArg(
          MBEAN_DIAGNOSTIC_REQUEST_HANDLER,
          "hasServiceInterface",
          FoodService.class.getName()))
          .thenReturn("false");
      assertThat(
          () -> service.getProxy(FoodService.class),
          is(throwing(instanceOf(DiagnosticOperationUnsupportedException.class)).andMessage(is(equalTo(FoodService.class.getName())))));
    }

    // getServiceInterface returns OK
    when(diagnostics.invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "hasServiceInterface", FoodService.class.getName())).thenReturn("true");
    FoodService foodService = service.getProxy(FoodService.class);

    // test proxy interactions with objects methods
    {
      reset(diagnostics);
      assertThat(foodService.equals(foodService), is(true));
      assertThat(foodService.hashCode(), is(equalTo(FoodService.class.hashCode())));
      assertThat(foodService.toString(), is(equalTo("DiagnosticServiceProxy(" + FoodService.class.getName() + ")")));
      verifyZeroInteractions(diagnostics);
    }

    // test invalid method (should not happen when using a proxy)
    {
      when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), request.capture())).thenReturn(MESSAGE_INVALID_JMX);
      assertThat(
          () -> foodService.cook(new Beef(2, false, "AA")),
          is(throwing(instanceOf(DiagnosticOperationExecutionException.class)).andMessage(is(equalTo(MESSAGE_INVALID_JMX)))));
    }

    // test no service found or no method found on service (see DiagnosticRequestHandler#findService)
    {
      when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), request.capture())).thenReturn(MESSAGE_UNKNOWN_COMMAND);
      assertThat(
          () -> foodService.cook(new Beef(2, false, "AA")),
          is(throwing(instanceOf(DiagnosticOperationUnsupportedException.class)).andMessage(is(equalTo(MESSAGE_UNKNOWN_COMMAND)))));
    }

    // test encoded request and OK success answer
    {
      DiagnosticResponse<Beef> diagnosticResponse = new DiagnosticResponse<>(new Beef(2, false, "AA"));
      String json = jsonCodec.serialize(diagnosticResponse);
      assertThat(json, is(equalTo("{\"body\":{\"@class\":\"org.terracotta.diagnostic.client.DiagnosticServiceImplTest$Beef\",\"quality\":\"AA\",\"raw\":false,\"time\":2}}")));
      String encodedResponse = new EmptyParameterDiagnosticCodec().around(new Base64DiagnosticCodec()).serialize(json);
      when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), request.capture())).thenReturn(encodedResponse);

      Beef in = new Beef(1, true, "AAA");
      Food out = foodService.cook(in);
      verify(jsonCodec).serialize(new DiagnosticRequest(FoodService.class, "cook", in));
      verify(jsonCodec).deserialize(json, DiagnosticResponse.class);
      assertThat(Json.toJsonTree(out), is(equalTo(Json.toJsonTree(diagnosticResponse.getBody()))));
    }

    // test encoded request and error success answer
    {
      reset(jsonCodec);
      DiagnosticResponse<Beef> diagnosticResponse = new DiagnosticResponse<>(null, new IllegalArgumentException("error message"));
      String json = jsonCodec.serialize(diagnosticResponse);
      assertThat(json, startsWith("{\"errorMessage\":\"error message\",\"errorStack\":\""));
      String encodedResponse = new EmptyParameterDiagnosticCodec().around(new Base64DiagnosticCodec()).serialize(json);
      when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), request.capture())).thenReturn(encodedResponse);

      Beef in = new Beef(1, true, "AAA");
      assertThat(
          () -> foodService.cook(in),
          is(throwing(instanceOf(DiagnosticOperationExecutionException.class)).andMessage(containsString(IllegalArgumentException.class.getName() + ": error message"))));
      verify(jsonCodec).serialize(new DiagnosticRequest(FoodService.class, "cook", in));
      verify(jsonCodec).deserialize(json, DiagnosticResponse.class);
    }
  }

  @Test
  public void test_different_codec() {
    Beef in = new Beef(1, true, "AAA");
    DiagnosticResponse<Beef> diagnosticResponse = new DiagnosticResponse<>(new Beef(2, false, "AA"));
    when(diagnostics.invokeWithArg(MBEAN_DIAGNOSTIC_REQUEST_HANDLER, "hasServiceInterface", FoodService.class.getName())).thenReturn("true");

    String encodedResponse = new EmptyParameterDiagnosticCodec().around(new Base64DiagnosticCodec()).around(jsonCodec).serialize(diagnosticResponse);
    when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), anyString())).thenReturn(encodedResponse);
    FoodService foodService = service.getProxy(FoodService.class);
    Food out = foodService.cook(in);

    encodedResponse = new EmptyParameterDiagnosticCodec().around(new Base64DiagnosticCodec()).around(javaCodec).serialize(diagnosticResponse);
    when(diagnostics.invokeWithArg(eq(MBEAN_DIAGNOSTIC_REQUEST_HANDLER), eq("request"), anyString())).thenReturn(encodedResponse);
    DiagnosticService javaService = DiagnosticServiceFactory.getDiagnosticService(connection, diagnostics, javaCodec);
    FoodService javaFoodService = javaService.getProxy(FoodService.class);

    assertThat(javaFoodService.cook(in), is(equalTo(out)));
  }


  public interface FoodService {
    Food cook(Food food);
  }

  public static abstract class Food implements Serializable {
    private static final long serialVersionUID = 1L;
    final int time;

    public Food(int time) {this.time = time;}

    public int getTime() {
      return time;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Food food = (Food) o;
      return time == food.time;
    }

    @Override
    public int hashCode() {
      return Objects.hash(time);
    }
  }

  public static abstract class Meat extends Food implements Serializable {
    private static final long serialVersionUID = 1L;
    final boolean raw;

    public Meat(int time, boolean raw) {
      super(time);
      this.raw = raw;
    }

    public boolean isRaw() {
      return raw;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Meat meat = (Meat) o;
      return raw == meat.raw;
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), raw);
    }
  }

  public static class Beef extends Meat implements Serializable {
    private static final long serialVersionUID = 1L;
    final String quality;

    @JsonCreator
    public Beef(@JsonProperty("time") int time,
                @JsonProperty("raw") boolean raw,
                @JsonProperty("quality") String quality) {
      super(time, raw);
      this.quality = quality;
    }

    public String getQuality() {
      return quality;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      if (!super.equals(o)) return false;
      Beef beef = (Beef) o;
      return Objects.equals(quality, beef.quality);
    }

    @Override
    public int hashCode() {
      return Objects.hash(super.hashCode(), quality);
    }
  }

}
