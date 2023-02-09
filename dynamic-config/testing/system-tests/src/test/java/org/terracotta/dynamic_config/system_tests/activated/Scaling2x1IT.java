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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.terracotta.angela.common.ToolExecutionResult;
import org.terracotta.dynamic_config.api.model.LockContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.UUID;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsOutput;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;
import static org.terracotta.dynamic_config.api.model.LockTag.DENY_SCALE_OUT;
import static org.terracotta.dynamic_config.api.model.LockTag.OWNER_PLATFORM;
import static org.terracotta.dynamic_config.api.model.LockTag.SCALE_IN_PREFIX;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 1)
@Ignore
public class Scaling2x1IT extends DynamicConfigIT {

  @Before
  public void setUp() {
    assertThat(
        configTool("activate", "-cluster-name", "my-cluster", "-stripe-shape", getNodeHostPort(1, 1).toString()),
        allOf(successful(), containsOutput("came back up")));

    waitForActive(1);
  }

  @SuppressWarnings("unchecked")
  @Test
  public void test_normalAttachDetach() {
    assertThat(configTool("attach", "-lock", "-unlock", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1).toString()), is(successful()));

    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 2"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : false")
        ));

    assertThat(configTool("detach", "-lock", "-unlock", "-from-cluster", getNodeHostPort(1, 1).toString(), "-stripe", getNodeHostPort(2, 1).toString()), is(successful()));

    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 1"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : false")
        ));
  }

  @Test
  public void test_rolledBackScaleOut() {
    // attach
    final ToolExecutionResult attach = configTool("attach", "-lock", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1).toString());
    assertThat(attach, allOf(is(successful()), containsOutput("Config lock with token: ")));

    final String token = attach.getOutput()
        .stream()
        .filter(line -> line.startsWith("Config lock with token: "))
        .map(line -> line.substring("Config lock with token: ".length()))
        .findFirst()
        .get();

    // simulated re-balancing failure: detach + marker + unlock
    assertThat(configTool("-lock-token", token, "detach", "-from-cluster", getNodeHostPort(1, 1).toString(), "-stripe", getNodeHostPort(2, 1).toString()), is(successful()));
    final LockContext marker = new LockContext(token, OWNER_PLATFORM, DENY_SCALE_OUT);
    assertThat(configTool("-lock-token", token, "lock-config", "-connect-to", getNodeHostPort(1, 1).toString(), "-lock-context", marker.toString()), allOf(is(successful()), containsOutput("Config lock with token: " + token)));
    assertThat(configTool("-lock-token", token, "unlock-config", "-connect-to", getNodeHostPort(1, 1).toString()), is(successful()));

    // verify that scale out is not allowed anymore
    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 1"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : true")
        ));

    // allow scale out
    assertThat(configTool("repair", "-force", "allow_scaling", "-connect-to", getNodeHostPort(1, 1).toString()), is(successful()));

    // verify that scale out is now allowed
    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 1"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : false")
        ));
  }

  @Test
  public void test_rolledBackScaleIn() {
    // create 2x1
    assertThat(configTool("attach", "-to-cluster", getNodeHostPort(1, 1).toString(), "-stripe-shape", getNodeHostPort(2, 1).toString()), is(successful()));
    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 2"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : false")
        ));

    // we simulate a failed detach: lock-marker-unlock
    final String token = UUID.randomUUID().toString();
    final UID stripeUID = getUpcomingCluster(1, 1).getStripes().get(1).getUID();
    final LockContext lock = new LockContext(token, OWNER_PLATFORM, SCALE_IN_PREFIX + stripeUID);
    assertThat(configTool("lock-config", "-connect-to", getNodeHostPort(1, 1).toString(), "-lock-context", lock.toString()), allOf(is(successful()), containsOutput("Config lock with token: " + token)));
    final LockContext marker = new LockContext(token, OWNER_PLATFORM, DENY_SCALE_OUT);
    assertThat(configTool("-lock-token", token, "lock-config", "-connect-to", getNodeHostPort(1, 1).toString(), "-lock-context", marker.toString()), allOf(is(successful()), containsOutput("Config lock with token: " + token)));
    assertThat(configTool("-lock-token", token, "unlock-config", "-connect-to", getNodeHostPort(1, 1).toString()), is(successful()));

    // verify that we scale in is not allowed anymore
    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 2"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : true")
        ));

    // allow scale in
    assertThat(configTool("repair", "-force", "allow_scaling", "-connect-to", getNodeHostPort(1, 1).toString()), is(successful()));

    // verify that scale in is now allowed
    assertThat(configTool("diagnostic", "-connect-to", getNodeHostPort(1, 1).toString(), "-output-format", "json"),
        allOf(
            successful(),
            containsOutput("\"stripes\" : 2"),
            containsOutput("\"configLocked\" : false"),
            containsOutput("\"manualInterventionRequired\" : false"),
            containsOutput("\"readyForTopologyChange\" : true"),
            containsOutput(" \"scalingDenied\" : false")
        ));
  }

}
