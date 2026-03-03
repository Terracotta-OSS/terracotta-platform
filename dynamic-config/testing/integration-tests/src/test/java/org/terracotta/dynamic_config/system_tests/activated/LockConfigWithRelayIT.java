/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2026
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
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class LockConfigWithRelayIT extends DynamicConfigIT {
  private final LockContext lockContext =
    new LockContext(UUID.randomUUID().toString(), "platform", "dynamic-scale");
  @Test
  public void testUnlockWithRelayNodes() throws Exception {
    startNode(1, 1);
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 1)).getNodeCount(), is(equalTo(1)));

    // start a second node
    startNode(1, 2);
    assertThat(configTool("set", "-s", "localhost:" + getNodePort(1, 2), "-c", "stripe.1.node.1.relay-mode=true", "-c", "stripe.1.node.1.replica-hostname=" + "localhost", "-c", "stripe.1.node.1.replica-port=" + "9410"), is(successful()));
    // attach
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(1, 1), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(getUpcomingCluster("localhost", getNodePort(1, 2)).getNodeCount(), is(equalTo(2)));
    activateCluster();
    waitForActive(1, 1);
    waitForPassiveRelay(1, 2);
    if (!lock()) {
      throw new AssertionError("lock failed");
    }
    unlock();
    assertThat(configTool("log", "-s", "localhost:" + getNodePort(1, 2)),
      allOf(is(successful()),
        containsOutput("Locking the config by 'platform (dynamic-scale)'"),
        containsOutput("Unlocking the config (forced=false)")));
  }

  private boolean lock() {
    ToolExecutionResult result = invokeWithoutToken("lock-config", "-s", "localhost:" + getNodePort(), "--lock-context", lockContext.toString());
    if (result.getExitStatus() != 0) {
      for (String line : result.getOutput()) {
        System.out.println("LOCK FAIL:" + line);
      }
      return false;
    } else {
      return true;
    }
  }

  private void unlock() {
    invokeWithToken("unlock-config", "-s", "localhost:" + getNodePort());
  }

  private ToolExecutionResult invokeWithoutToken(String... args) {
    return configTool(args);
  }

  private void invokeWithToken(String... args) {
    List<String> newArgs = new ArrayList<>(asList("--lock-token", lockContext.getToken()));
    newArgs.addAll(asList(args));
    assertThat(configTool(newArgs.toArray(new String[0])), is(successful()));
  }
}
