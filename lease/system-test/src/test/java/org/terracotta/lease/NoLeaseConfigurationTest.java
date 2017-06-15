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
package org.terracotta.lease;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.passthrough.PassthroughClusterControl;
import org.terracotta.passthrough.PassthroughTestHelpers;

import java.net.URI;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.fail;

public class NoLeaseConfigurationTest {
  private static PassthroughClusterControl cluster;

  @BeforeClass
  public static void beforeClass() {
    cluster = PassthroughTestHelpers.createActiveOnly("stripe",
            server -> {
              server.registerClientEntityService(new LeaseAcquirerClientService());
              server.registerServerEntityService(new LeaseAcquirerServerService());
            });

    cluster.startAllServers();
  }

  @AfterClass
  public static void afterClass() throws Exception {
    cluster.tearDown();
  }

  @Test
  public void defaultConfigurationAllowsALease() throws Exception {
    URI clusterURI = URI.create("passthrough://stripe");
    Connection connection = ConnectionFactory.connect(clusterURI, new Properties());
    LeaseMaintainer leaseMaintainer = LeaseMaintainerFactory.createLeaseMaintainer(connection);

    LeaseTestUtil.waitForValidLease(leaseMaintainer);
  }
}
