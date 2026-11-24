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

import org.junit.Test;
import org.terracotta.testing.config.ConfigRepoStartupBuilder;
import org.terracotta.testing.rules.BasicExternalCluster;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

import static org.terracotta.testing.rules.BasicExternalClusterBuilder.newCluster;

public class GalvanDiagnosticsStateIT {

  private static final String NAME = "GalvanDiagnosticsStateIT";
  private static final String RESOURCE_CONFIG =
      "<config xmlns:ohr='http://www.terracotta.org/config/offheap-resource'>"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"main\" unit=\"MB\">512</ohr:resource>"
          + "<ohr:resource name=\"foo\" unit=\"GB\">1</ohr:resource>"
          + "</ohr:offheap-resources>"
          + "</config>\n";

  @Test
  public void activeServerReportsActiveCoordinatorState() throws Exception {
    BasicExternalCluster cluster = (BasicExternalCluster) newCluster()
        .inline(false)
        .withServerHeap(512)
        .withClientReconnectWindowTime(10)
        .withServiceFragment(RESOURCE_CONFIG)
        .startupBuilder(ConfigRepoStartupBuilder::new)
        .in(Paths.get("build", "galvan", "tests", NAME))
        .build();

    long startNanos = System.nanoTime();
    CompletableFuture<Void> failure = cluster.manualStart(NAME).toCompletableFuture();

    try {
      if (failure.isCompletedExceptionally()) {
        failure.join();
      }

      cluster.getClusterControl().waitForActive();
      URI uri = cluster.getConnectionURI();
      DiagnosticsRootStateAssert.assertActiveCoordinator(
          new InetSocketAddress(uri.getHost(), uri.getPort()));

      long readyNanos = System.nanoTime();
      System.out.println(
          "FRAMEWORK_PERF framework=galvan ready_ns=" + (readyNanos - startNanos));
    } finally {
      cluster.manualStop();
    }
  }
}
