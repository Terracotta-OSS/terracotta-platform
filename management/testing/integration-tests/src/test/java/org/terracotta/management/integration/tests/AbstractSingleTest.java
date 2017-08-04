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
import org.junit.Rule;
import org.terracotta.testing.rules.Cluster;

import java.io.File;

import static org.terracotta.testing.rules.BasicExternalClusterBuilder.newCluster;

/**
 * @author Mathieu Carbou
 */
public abstract class AbstractSingleTest extends AbstractTest {

  private final String offheapResource = "primary-server-resource";
  private final String resourceConfig =
      "<config xmlns:ohr='http://www.terracotta.org/config/offheap-resource'>"
          + "<ohr:offheap-resources>"
          + "<ohr:resource name=\"" + offheapResource + "\" unit=\"MB\">64</ohr:resource>"
          + "</ohr:offheap-resources>" +
          "</config>\n";

  @Rule
  public Cluster voltron =
      newCluster().in(new File("target/galvan")).withServiceFragment(resourceConfig).build();

  @Before
  public void setUp() throws Exception {
    voltron.getClusterControl().waitForActive();
    commonSetUp(voltron);
  }

  @After
  public void tearDown() throws Exception {
    commonTearDown();
  }

}
