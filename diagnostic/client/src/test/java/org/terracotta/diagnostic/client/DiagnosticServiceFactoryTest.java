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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.ConnectionService;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.exception.EntityNotProvidedException;
import org.terracotta.exception.EntityVersionMismatchException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.connection.Diagnostics;

/**
 * @author Mathieu Carbou
 */
@RunWith(MockitoJUnitRunner.class)
public class DiagnosticServiceFactoryTest {

  @Rule public ExpectedException exception = ExpectedException.none();
  @Mock public ConnectionService connectionService;
  @Mock public Connection connection;
  @Mock public Diagnostics diagnostics;
  @Mock public EntityRef<Diagnostics, Object, Properties> entityRef;
  @Captor public ArgumentCaptor<Properties> properties;
  @Captor public ArgumentCaptor<Iterable<InetSocketAddress>> address;

  @Before
  public void setUp() throws Exception {
    when(connectionService.connect(address.capture(), properties.capture())).thenReturn(connection);
  }

  @Test
  public void test_fetch() throws ConnectionException, EntityVersionMismatchException, EntityNotProvidedException, EntityNotFoundException {
    when(connection.<Diagnostics, Object, Properties>getEntityRef(eq(Diagnostics.class), eq(1L), eq("root"))).thenReturn(entityRef);
    when(entityRef.fetchEntity(any(Properties.class))).thenReturn(diagnostics);

    InetSocketAddress addr = InetSocketAddress.createUnresolved("localhost", 9410);
    DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        connectionService,
        addr,
        "ConnectionName",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        null);

    assertThat(diagnosticService, is(not(nullValue())));
    assertThat(address.getValue().iterator().next(), is(equalTo(addr)));
    assertThat(properties.getValue(), hasEntry(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic"));
    assertThat(properties.getValue(), hasEntry(ConnectionPropertyNames.CONNECTION_NAME, "ConnectionName"));
    assertThat(properties.getValue(), hasEntry(ConnectionPropertyNames.CONNECTION_TIMEOUT, "2000"));
  }

  @Test
  public void test_connection_closed_on_ref_failure() throws ConnectionException, EntityNotProvidedException, IOException {
    when(connection.<Diagnostics, Object, Properties>getEntityRef(eq(Diagnostics.class), eq(1L), eq("root"))).thenThrow(new EntityNotProvidedException("", ""));

    exception.expect(ConnectionException.class);

    DiagnosticServiceFactory.fetch(
        connectionService,
        InetSocketAddress.createUnresolved("localhost", 9410),
        "ConnectionName",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        null);

    verify(connection, times(1)).close();
  }

  @Test
  public void test_connection_closed_on_fetch_failure() throws ConnectionException, EntityVersionMismatchException, EntityNotProvidedException, EntityNotFoundException, IOException {
    when(connection.<Diagnostics, Object, Properties>getEntityRef(eq(Diagnostics.class), eq(1L), eq("root"))).thenReturn(entityRef);
    when(entityRef.fetchEntity(any(Properties.class))).thenThrow(new EntityNotFoundException("", ""));

    exception.expect(ConnectionException.class);

    DiagnosticServiceFactory.fetch(
        connectionService,
        InetSocketAddress.createUnresolved("localhost", 9410),
        "ConnectionName",
        Duration.ofSeconds(2),
        Duration.ofSeconds(3),
        null);

    verify(connection, times(1)).close();
  }

}