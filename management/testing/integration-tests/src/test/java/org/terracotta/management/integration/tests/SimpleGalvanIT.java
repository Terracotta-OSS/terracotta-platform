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

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.terracotta.management.entity.sample.client.CacheFactory;
import org.terracotta.management.registry.collect.StatisticConfiguration;
import org.terracotta.testing.rules.BasicExternalCluster;
import org.terracotta.testing.rules.Cluster;

import java.io.File;
import java.net.URI;
import java.util.concurrent.TimeUnit;

import static java.util.Collections.emptyList;

public class SimpleGalvanIT {

  private static final String OFFHEAP_RESOURCE = "primary-server-resource";

  private static final String RESOURCE_CONFIG =
      "<config xmlns:ohr='http://www.terracotta.org/config/offheap-resource'>"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"" + OFFHEAP_RESOURCE + "\" unit=\"MB\">64</ohr:resource>"
          + "</ohr:offheap-resources>" +
          "</config>\n";

  @ClassRule
  public static Cluster CLUSTER =
      new BasicExternalCluster(new File("target/galvan"), 1, emptyList(), "", RESOURCE_CONFIG, "");

  @BeforeClass
  public static void waitForActive() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
  }

  @Rule
  public Timeout timeout = Timeout.seconds(30);

  CacheFactory cacheFactory;

  @Before
  public void setUp() throws Exception {
    StatisticConfiguration statisticConfiguration = new StatisticConfiguration()
        .setAverageWindowDuration(1, TimeUnit.MINUTES)
        .setHistorySize(100)
        .setHistoryInterval(1, TimeUnit.SECONDS)
        .setTimeToDisable(5, TimeUnit.SECONDS);
    URI uri = CLUSTER.getConnectionURI().resolve("/pif");
    cacheFactory = new CacheFactory(uri, statisticConfiguration);
  }

  @After
  public void tearDown() throws Exception {
    cacheFactory.getConnection().close();
  }

  @Test
  public void simpleTest_one_active() throws Exception {
    cacheFactory.init(); // create and fetches management entity
    cacheFactory.getCache("paf"); // create and fetch sample entity
  }
}
