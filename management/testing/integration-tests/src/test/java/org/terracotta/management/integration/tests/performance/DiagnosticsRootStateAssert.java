/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.management.integration.tests.performance;

import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.Diagnostics;

import java.net.InetSocketAddress;
import java.util.Properties;

import static java.util.Collections.singletonList;

final class DiagnosticsRootStateAssert {

  private static final String EXPECTED_STATE = "ACTIVE-COORDINATOR";

  private DiagnosticsRootStateAssert() {
  }

  static void assertActiveCoordinator(InetSocketAddress address) throws Exception {
    Properties props = new Properties();
    props.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");
    try (Connection connection = ConnectionFactory.connect(singletonList(address), props);
         Diagnostics diagnostics = connection.<Diagnostics, Object, Void>getEntityRef(Diagnostics.class, 1, "root").fetchEntity(null)) {
      String state = diagnostics.getState();
      if (!EXPECTED_STATE.equals(state)) {
        throw new AssertionError("Expected root diagnostics state <" + EXPECTED_STATE + "> but was <" + state + ">");
      }
    }
  }
}
