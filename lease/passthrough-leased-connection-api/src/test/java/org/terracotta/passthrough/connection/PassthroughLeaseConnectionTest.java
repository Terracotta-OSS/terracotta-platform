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
package org.terracotta.passthrough.connection;

import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.terracotta.lease.connection.LeasedConnection;
import org.terracotta.lease.connection.LeasedConnectionFactory;
import org.terracotta.passthrough.IAsynchronousServerCrasher;
import org.terracotta.passthrough.PassthroughServer;
import org.terracotta.passthrough.PassthroughServerRegistry;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

public class PassthroughLeaseConnectionTest {

  @Test
  public void testLeasedConnectionInPassthrough() throws Exception {

    PassthroughServer passthroughServer = new PassthroughServer();
    passthroughServer.setServerName("localhost");
    passthroughServer.setBindPort(8510);
    passthroughServer.registerAsynchronousServerCrasher(mock(IAsynchronousServerCrasher.class));
    passthroughServer.start(true, false);
    PassthroughServerRegistry.getSharedInstance().registerServer("localhost", passthroughServer);

    LeasedConnection connection = LeasedConnectionFactory.connect(URI.create("passthrough://localhost:8510"), new Properties());
    assertThat(connection, CoreMatchers.instanceOf(PassthroughLeasedConnectionService.PassthroughLeasedConnection.class));
  }
}
