/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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

import org.mockito.Mockito;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Properties;

public class TestLeasedConnectionService implements LeasedConnectionService {

  public static final LeasedConnection LEASED_CONNECTION = Mockito.mock(LeasedConnection.class);

  @Override
  public boolean handlesURI(URI uri) {
    return true;
  }

  @Override
  public boolean handlesConnectionType(String connectionType) {
    return true;
  }

  @Override
  public LeasedConnection connect(URI uri, Properties properties) {
    return LEASED_CONNECTION;
  }

  @Override
  public LeasedConnection connect(Iterable<InetSocketAddress> servers, Properties properties) {
    return LEASED_CONNECTION;
  }
}
