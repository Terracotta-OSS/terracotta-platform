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

import java.util.concurrent.CountDownLatch;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class DetachCommandWithVoter1x2IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(1);
  }

  @Test
  public void testDetachAndVerifyWithVoter() throws InterruptedException {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    String active = getNode(1, activeId).getHostPort();
    String passive = getNode(1, passiveId).getHostPort();

    try (ActiveVoter activeVoter = new ActiveVoter("voter1", active, passive)) {

      CountDownLatch voted = new CountDownLatch(1);
      activeVoter.setVoteListener(s -> {
        if (s.equals(active)) {
          voted.countDown();
        }
      });

      activeVoter.startAndAwaitRegistrationWithAll();

      stopNode(1, passiveId);
      voted.await();

      waitUntil(()->configTool("detach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }

  @Test
  public void testDetachAndAttachVerifyWithVoter() {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    String active = getNode(1, activeId).getHostPort();
    String passive = getNode(1, passiveId).getHostPort();

    try (ActiveVoter activeVoter1 = new ActiveVoter("voter1", active, passive)) {
      activeVoter1.startAndAwaitRegistrationWithAll();

      waitUntil(() -> configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      waitUntil(activeVoter1::getKnownHosts, is(1));

      startNode(1, passiveId);

      waitUntil(()->configTool("attach", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      waitUntil(activeVoter1::getKnownHosts, is(2));

      withTopologyService(1, activeId, topologyService -> assertTrue(topologyService.isActivated()));
      withTopologyService(1, passiveId, topologyService -> assertTrue(topologyService.isActivated()));
    }
  }
}
