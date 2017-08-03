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
import org.terracotta.testing.rules.Cluster;

import java.io.File;
import java.net.URI;

import static org.terracotta.testing.rules.BasicExternalClusterBuilder.newCluster;

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
      newCluster().in(new File("target/galvan")).withServiceFragment(RESOURCE_CONFIG).build();

  @BeforeClass
  public static void waitForActive() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
  }

  @Rule
  public Timeout timeout = Timeout.seconds(30);

  CacheFactory cacheFactory;

  @Before
  public void setUp() throws Exception {
    URI uri = CLUSTER.getConnectionURI().resolve("/pif");
    cacheFactory = new CacheFactory(uri);
  }

  @After
  public void tearDown() throws Exception {
    cacheFactory.getConnection().close();
  }

  @Test
  public void simpleTest_one_active() throws Exception {
    cacheFactory.init(); // create and fetches NMS Agent Entity
    cacheFactory.getCache("paf"); // create and fetch sample entity
  }
}
