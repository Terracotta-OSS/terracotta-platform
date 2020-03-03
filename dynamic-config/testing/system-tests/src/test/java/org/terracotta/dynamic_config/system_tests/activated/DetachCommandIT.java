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
package org.terracotta.dynamic_config.system_tests.activated;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.system_tests.ClusterDefinition;
import org.terracotta.dynamic_config.system_tests.DynamicConfigIT;
import org.terracotta.dynamic_config.system_tests.util.NodeOutputRule;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.containsOutput;
import static org.terracotta.dynamic_config.system_tests.util.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommandIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void test_detach_active_node() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    out.clearLog(1, activeId);
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, passiveId), "-s", "localhost:" + getNodePort(1, activeId)), is(successful()));

    // the detached node becomes active in its own cluster
    waitUntil(out.getLog(1, activeId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    // failover - existing passive becomes active
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    assertTopologyChanged(activeId, passiveId);
  }

  @Test
  public void test_detach_passive_from_activated_cluster() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    out.clearLog(1, passiveId);
    assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    assertTopologyChanged(activeId, passiveId);
  }

  @Test
  public void test_detach_passive_from_activated_cluster_requiring_restart() throws Exception {
    final int activeId = findActive(1).getAsInt();
    final int passiveId = findPassives(1)[0];

    // do a change requiring a restart
    assertThat(configToolInvocation("set", "-s", "localhost:" + getNodePort(1, activeId), "-c", "stripe.1.node.1.tc-properties.foo=bar"),
        allOf(is(successful()), containsOutput("IMPORTANT: A restart of the cluster is required to apply the changes")));

    // try to detach this node
    assertThat(
        configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)),
        containsOutput("Impossible to do any topology change. Cluster at address: " + "localhost:" + getNodePort(1, activeId) +
            " is waiting to be restarted to apply some pending changes. " +
            "You can run the command with -f option to force the comment but at the risk of breaking this cluster configuration consistency. " +
            "The newly added node will be restarted, but not the existing ones."));

    // try forcing the detach
    out.clearLog(1, passiveId);
    assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));
    waitUntil(out.getLog(1, passiveId), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    assertTopologyChanged(activeId, passiveId);
  }

  private void assertTopologyChanged(int activeId, int passiveId) throws Exception {
    assertThat(getUpcomingCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, activeId)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, activeId))));

    assertThat(getUpcomingCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getNodeCount(), is(equalTo(1)));
    assertThat(getRuntimeCluster("localhost", getNodePort(1, passiveId)).getSingleNode().get().getNodePort(), is(equalTo(getNodePort(1, passiveId))));
  }
}
