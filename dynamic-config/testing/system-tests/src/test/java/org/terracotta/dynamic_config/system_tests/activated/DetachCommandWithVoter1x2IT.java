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

import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.voter.ActiveVoter;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.terracotta.dynamic_config.test_support.angela.AngelaMatchers.successful;
import static org.terracotta.utilities.test.WaitForAssert.assertThatEventually;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommandWithVoter1x2IT extends DynamicConfigIT {

  public DetachCommandWithVoter1x2IT() {
    super(Duration.ofSeconds(180));
  }

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(1);
  }

  @Test
  public void testDetachAndVerifyWithVoter() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      // To ensure voter connects to all the servers 
      Thread.sleep(10000);

      stopNode(1, passiveId);
      assertThat(configToolInvocation("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      String[] nodes = new String[]{getNode(1, activeId).getHostPort()};

      assertThatEventually(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      assertThatEventually(() -> activeVoter.getHeartbeatFutures().size(), is(1));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }

  @Test
  public void testDetachAndAttachVerifyWithVoter() throws Exception {
    int activeId = findActive(1).getAsInt();
    int passiveId = findPassives(1)[0];

    try (ActiveVoter activeVoter = new ActiveVoter("mvoter", new CompletableFuture<>(), Optional.empty(), getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort())) {
      activeVoter.start();

      // To ensure voter connects to all the servers 
      Thread.sleep(10000);

      assertThat(configToolInvocation("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      String[] nodes = new String[]{getNode(1, activeId).getHostPort()};

      assertThatEventually(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      assertThatEventually(() -> activeVoter.getHeartbeatFutures().size(), is(1));

      startNode(1, passiveId);
      waitForDiagnostic(1, passiveId);

      assertThat(configToolInvocation("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      nodes = new String[]{getNode(1, passiveId).getHostPort()};
      assertThatEventually(activeVoter::getExistingTopology, containsInAnyOrder(nodes));
      assertThatEventually(() -> activeVoter.getHeartbeatFutures().size(), is(2));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }
}
