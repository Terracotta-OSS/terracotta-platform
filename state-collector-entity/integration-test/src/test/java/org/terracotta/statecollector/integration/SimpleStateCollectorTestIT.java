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
package org.terracotta.statecollector.integration;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.terracotta.statecollector.StateCollector;
import org.terracotta.statecollector.StateCollectorConstants;
import org.terracotta.testing.rules.BasicExternalCluster;
import org.terracotta.testing.rules.Cluster;

import java.io.File;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SimpleStateCollectorTestIT {

  @ClassRule
  public static Cluster CLUSTER =
      new BasicExternalCluster(new File("target/galvan"), 1, emptyList(), "", "", "");

  @BeforeClass
  public static void waitForActive() throws Exception {
    CLUSTER.getClusterControl().waitForActive();
  }

  @Test
  public void testStateCollectorFetch() {
    try {
      StateCollector stateCollector = CLUSTER.newConnection().
          getEntityRef(StateCollector.class, StateCollectorConstants.STATE_COLLECTOR_VERSION, StateCollectorConstants.STATE_COLLECTOR_ENTITY_NAME).
          fetchEntity(null);
      assertThat(stateCollector, is(notNullValue()));
      stateCollector.close();
    } catch (Exception e) {
      Assert.fail("StateCollectorFetch failed due to " + e.getLocalizedMessage());
    }
  }
}
