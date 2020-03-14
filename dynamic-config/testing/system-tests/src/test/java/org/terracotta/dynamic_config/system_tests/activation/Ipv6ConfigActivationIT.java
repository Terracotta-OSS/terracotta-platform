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
package org.terracotta.dynamic_config.system_tests.activation;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigRepositoryGenerator;
import org.terracotta.dynamic_config.test_support.util.NodeOutputRule;

import java.nio.file.Path;

import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsLog;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.containsOutput;

@ClusterDefinition(nodesPerStripe = 2, autoStart = false)
public class Ipv6ConfigActivationIT extends DynamicConfigIT {

  @Rule public final NodeOutputRule out = new NodeOutputRule();

  @Test
  public void testStartupFromMigratedConfigRepoAndGetCommand() throws Exception {
    Path configurationRepo = generateNodeRepositoryDir(1, 1, ConfigRepositoryGenerator::generate1Stripe1NodeIpv6);
    startNode(1, 1, "--node-repository-dir", configurationRepo.toString());
    waitUntil(out.getLog(1, 1), containsLog("Moved to State[ ACTIVE-COORDINATOR ]"));

    assertThat(configToolInvocation("get", "-s", "[::1]:" + getNodePort(), "-c", "offheap-resources.main"), containsOutput("offheap-resources.main=512MB"));
  }
}
