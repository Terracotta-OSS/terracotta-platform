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
@ClusterDefinition
public class DiagnosticIT extends DynamicConfigIT {

  @Override
  protected void startNode(int stripeId, int nodeId) {
    startNode(1, 1,
        "--node-repository-dir", getNodePath(stripeId, nodeId).resolve("repository").toString(),
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
        null)
    ) {
      assertThat(diagnosticService.getLogicalServerState(), is(equalTo(LogicalServerState.STARTING)));
    }
  }

}
