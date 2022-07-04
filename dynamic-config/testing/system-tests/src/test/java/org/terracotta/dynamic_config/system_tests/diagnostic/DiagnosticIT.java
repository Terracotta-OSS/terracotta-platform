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
      System.out.println(diagnosticService.getVersion()); // previously existing call, returns something like "Terracotta 5.8.2-pre6"
      System.out.println(diagnosticService.getBuildID()); // previously existing call, returns something like "2021-06-29 at 20:54:46 UTC (Revision 4450fe6fc2c174abd3528b8636b3296a6a79df00 from UNKNOWN)"
      System.out.println(diagnosticService.getKitInformation()); // new call, returns structured object that has a properties-like representation
      /*
version=5.8.2-pre6
revision=4450fe6fc2c174abd3528b8636b3296a6a79df00
branch=UNKNOWN
timestamp=2021-06-29T20\:54\:46Z
       */
      System.out.println(diagnosticService.getKitInformation().getVersion()); // i.e.: 5.8.2-pre6
      System.out.println(diagnosticService.getKitInformation().getBranch()); // i.e.: master
      System.out.println(diagnosticService.getKitInformation().getRevision()); // i.e.: 4450fe6fc2c174abd3528b8636b3296a6a79df00
      System.out.println(diagnosticService.getKitInformation().getTimestamp()); // i.e.: 2021-06-29T20:54:46Z
    }
  }

}
