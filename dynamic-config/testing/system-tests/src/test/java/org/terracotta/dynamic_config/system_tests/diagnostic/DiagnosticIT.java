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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import org.junit.Test;
import org.terracotta.diagnostic.client.DiagnosticService;
import org.terracotta.diagnostic.client.DiagnosticServiceFactory;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.dynamic_config.api.model.TerracottaKit;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.time.Duration;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(failoverPriority = "")
public class DiagnosticIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    startNode(1, 1,
        "--config-dir", "config",
        "-f", copyConfigProperty("/config-property-files/single-stripe.properties").toString()
    );
  }

  @Test
  public void test_access_logical_server_state() throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        getNodeAddress(1, 1),
        getClass().getSimpleName(),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        null,
        objectMapperFactory)
    ) {
      assertThat(diagnosticService.getLogicalServerState(), is(equalTo(LogicalServerState.DIAGNOSTIC)));
    }
  }

  @Test
  public void test_get_kit_information() throws Exception {
    try (DiagnosticService diagnosticService = DiagnosticServiceFactory.fetch(
        getNodeAddress(1, 1),
        getClass().getSimpleName(),
        Duration.ofSeconds(5),
        Duration.ofSeconds(5),
        null,
        objectMapperFactory)
    ) {
      TopologyService topologyService = diagnosticService.getProxy(TopologyService.class);
      TerracottaKit terracottaKit = topologyService.getTerracottaKit();
      System.out.println(terracottaKit);
      terracottaKit.getInstalledComponents().forEach(System.out::println);
      // output:
/*
Terracotta 5.8.2-pre5, as of 2021-06-22 at 19:40:16 UTC (Revision f61e7ba47428e3bc5703c6071169755ed83b1ac2 from UNKNOWN)
Config : Dynamic                    5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Entity : Cluster Topology           5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Entity : Monitoring Agent           5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Entity : Monitoring Platform        5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Entity : Nomad                      5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Plugin : Off-Heap                   5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Client Message Tracker     5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Configuration Monitoring   5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Diagnostic Communication   5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Dynamic Configuration      5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Lease                      5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Monitoring                 5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
Service: Server Information         5.8.0.SNAPSHOT  (built on 2021-06-25T03:55:36Z with JDK 1.8.0_282)
*/
    }
  }

}
