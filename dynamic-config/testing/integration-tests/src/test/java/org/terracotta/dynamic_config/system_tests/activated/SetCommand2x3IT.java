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
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.function.Predicate.isEqual;
import static java.util.stream.IntStream.rangeClosed;
import static java.util.stream.Stream.concat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2, nodesPerStripe = 3, autoActivate = true)
public class SetCommand2x3IT extends DynamicConfigIT {

  @Test
  public void testAutoRestart() {
    int passiveId = waitForNPassives(1, 1)[0];
    String exclude = "stripe.1.node." + passiveId + ".tc-properties.foo=bar";

    String[] cli = concat(
        Stream.of(
            "set",
            "-auto-restart",
            "-connect-to", "localhost:" + getNodePort(2, 1)),
        rangeClosed(1, 2).boxed().flatMap(stripeId -> rangeClosed(1, 3)
            .mapToObj(nodeId -> "stripe." + stripeId + ".node." + nodeId + ".tc-properties.foo=bar"))
            .filter(isEqual(exclude).negate())
            .flatMap(cfg -> Stream.of("-setting", cfg))
    ).toArray(String[]::new);

    assertThat(configTool(cli), both(is(successful())).and(containsOutput("Restart required for nodes:")));

    for (int s = 1; s <= 2; s++) {
      for (int n = 1; n <= 3; n++) {
        assertFalse(usingTopologyService(s, n, TopologyService::mustBeRestarted));
      }
    }
  }

  @Test
  public void testSetMultipleRelayModeWithAutoRestart() {
    waitForActive(1);
    waitForActive(2);

    int basePort = 9410;

    List<String> args = new ArrayList<>(List.of("set", "-auto-restart", "-connect-to", "localhost:" + getNodePort(1, 1)));

    for (int stripeId = 1; stripeId <= 2; stripeId++) {
      for (int nodeId = 2; nodeId <= 3; nodeId++) {
        String prefix = "stripe." + stripeId + ".node." + nodeId;
        args.addAll(List.of("-setting", prefix + ".relay-mode=true"));
        args.addAll(List.of("-setting", prefix + ".replica-hostname=localhost"));
        args.addAll(List.of("-setting", prefix + ".replica-port=" + (basePort++)));
      }
    }

    String[] cli = args.toArray(String[]::new);

    assertThat(configTool(cli), is(successful()));

    waitForPassiveRelay(1, 2);
    waitForPassiveRelay(1, 3);
    waitForPassiveRelay(2, 2);
    waitForPassiveRelay(2, 3);
  }
}
