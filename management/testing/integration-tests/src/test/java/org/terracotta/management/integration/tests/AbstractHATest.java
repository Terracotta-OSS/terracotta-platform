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
package org.terracotta.management.integration.tests;

import java.nio.file.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;
import org.terracotta.management.model.cluster.AbstractManageableNode;
import org.terracotta.management.model.cluster.Server;
import org.terracotta.testing.rules.Cluster;

import java.nio.file.Paths;
import static org.terracotta.testing.config.ConfigConstants.DEFAULT_CLUSTER_NAME;
import org.terracotta.testing.config.ConfigRepoStartupBuilder;

import static org.terracotta.testing.rules.BasicExternalClusterBuilder.newCluster;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractHATest extends AbstractTest {

  private final String offheapResource = "primary-server-resource";
  private final String resourceConfig =
      "<config xmlns:ohr='http://www.terracotta.org/config/offheap-resource'>"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"" + offheapResource + "\" unit=\"MB\">64</ohr:resource>"
          + "</ohr:offheap-resources>" +
          "</config>\n";

  @Rule
  public Cluster voltron = newCluster(2)
      .in(Paths.get("target" ,"galvan"))
      .withServiceFragment(resourceConfig)
      .withSystemProperty("terracotta.management.assert", "true")
      .withTcProperty("terracotta.management.assert", "true")
      .inline(false)
      .startupBuilder(ConfigRepoStartupBuilder::new)
      .build();

  @Rule
  public TestName testName = new TestName();

  @Before
  @SuppressWarnings("rawtypes")
  public void setUp() throws Exception {
    System.out.println(" => [" + testName.getMethodName() + "] " + getClass().getSimpleName() + ".setUp()");
    voltron.getClusterControl().waitForActive();
    voltron.getClusterControl().waitForRunningPassivesInStandby();
    commonSetUp(voltron);
    nmsService.readMessages();

    // this is to wait for all passives to have exposed their management registry through the non-reliable communication channel (passive -> active)
    while (!Thread.currentThread().isInterrupted() && nmsService.readTopology()
        .serverStream()
        .filter(server -> !server.isActive())
        .flatMap(Server::serverEntityStream)
        .filter(AbstractManageableNode::isManageable)
        .count() != 4) {
      Thread.sleep(1_000);
    }
  }

  @After
  public void tearDown() throws Exception {
    System.out.println(" => [" + testName.getMethodName() + "] " + getClass().getSimpleName() + ".tearDown()");
    commonTearDown();
  }

}
