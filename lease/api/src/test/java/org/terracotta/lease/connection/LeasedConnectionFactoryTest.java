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
package org.terracotta.lease.connection;

import org.junit.Test;
import org.terracotta.connection.ConnectionException;

import java.net.URI;

import static org.junit.Assert.*;

public class LeasedConnectionFactoryTest {

  @Test
  public void connect() throws ConnectionException {
    LeasedConnection leasedConnection = LeasedConnectionFactory.connect(URI.create("terracotta://localhost"), null);

    assertSame(leasedConnection, TestLeasedConnectionService.LEASED_CONNECTION);
  }
}