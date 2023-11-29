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

import org.hamcrest.CoreMatchers;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.FailoverPriority;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.voter.TCVoter;
import org.terracotta.voter.TCVoterImpl;
import org.terracotta.voter.VoterStatus;
import org.terracotta.voter.VotingGroup;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoActivate = true)
public class Voter1x2IT extends DynamicConfigIT {

  @Override
  protected FailoverPriority getFailoverPriority() {
    return FailoverPriority.consistency(1);
  }

  @Test
  public void testDirectConnection() throws Exception {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];

    TCVoter voter = new TCVoterImpl();
    VoterStatus voterStatus = voter.register("MyCluster", getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort());
    voterStatus.awaitRegistrationWithAll(10, TimeUnit.SECONDS);

    stopNode(1, activeId);
    CompletableFuture<Void> connectionFuture = CompletableFuture.runAsync(() -> {
      try {
        waitForActive(1);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
    connectionFuture.get(10, TimeUnit.SECONDS);
  }

  @Test
  public void testTopologyUpdateAfterPassiveRemoval() throws Exception {
    int activeId = waitForActive(1);
    int passiveId = waitForNPassives(1, 1)[0];
    String active = getNode(1, activeId).getHostPort();
    String passive = getNode(1, passiveId).getHostPort();

    final Properties props = new Properties();
    props.setProperty("request.timeout", "10000");

    try (VotingGroup activeVoter = new VotingGroup("voter1", active, passive)) {
      activeVoter.start().awaitRegistrationWithAll();

      String[] hostPorts = {getNode(1, activeId).getHostPort(), getNode(1, passiveId).getHostPort()};
      Set<String> expectedTopology = new HashSet<>(Arrays.asList(hostPorts));

      assertThat(activeVoter.getExistingTopology(), CoreMatchers.is(expectedTopology));
      assertThat(activeVoter.countConnectedServers(), CoreMatchers.is(hostPorts.length));

      CountDownLatch voted = new CountDownLatch(1);
      activeVoter.addVotingListener(s -> {
        if (s.equals(active)) {
          voted.countDown();
        }
      });

      stopNode(1, passiveId);
      voted.await();
      waitForActive(1);
      assertThat(configTool("detach", "-f", "-d", "localhost:" + getNodePort(1, activeId), "-s", "localhost:" + getNodePort(1, passiveId)), is(successful()));

      expectedTopology.remove(hostPorts[1]);
      waitUntil(activeVoter::getExistingTopology, is(expectedTopology));
      waitUntil(activeVoter::countConnectedServers, is(1));
    }
  }
}
