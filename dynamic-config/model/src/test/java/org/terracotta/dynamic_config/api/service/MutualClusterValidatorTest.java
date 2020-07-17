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
package org.terracotta.dynamic_config.api.service;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.terracotta.dynamic_config.api.model.Cluster;

import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.dynamic_config.api.model.Cluster.newDefaultCluster;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;

public class MutualClusterValidatorTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testClusterNameMismatch() {
    Cluster one = newDefaultCluster().setName("one");
    Cluster two = newDefaultCluster().setName("two");
    assertClusterValidationFails("Mismatch found in cluster-name", one, two);
  }

  @Test
  public void testClientLeaseDurationMismatch() {
    Cluster one = newDefaultCluster().setClientLeaseDuration(1, SECONDS);
    Cluster two = newDefaultCluster().setClientLeaseDuration(100, SECONDS);
    assertClusterValidationFails("Mismatch found in client-lease-duration", one, two);
  }

  @Test
  public void testClientReconnectWindowMismatch() {
    Cluster one = newDefaultCluster().setClientReconnectWindow(1, SECONDS);
    Cluster two = newDefaultCluster().setClientReconnectWindow(100, SECONDS);
    assertClusterValidationFails("Mismatch found in client-reconnect", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_availabilityVsConsistency() {
    Cluster one = newDefaultCluster().setFailoverPriority(availability());
    Cluster two = newDefaultCluster().setFailoverPriority(consistency());
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_availabilityVsConsistencyVoter() {
    Cluster one = newDefaultCluster().setFailoverPriority(availability());
    Cluster two = newDefaultCluster().setFailoverPriority(consistency(1));
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_consistencyVsConsistencyVoter() {
    Cluster one = newDefaultCluster().setFailoverPriority(consistency());
    Cluster two = newDefaultCluster().setFailoverPriority(consistency(1));
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testOffheapNameMismatch() {
    Cluster one = newDefaultCluster().setOffheapResource("first", 1, GB);
    Cluster two = newDefaultCluster().setOffheapResource("second", 1, GB);
    assertClusterValidationFails("Mismatch found in offheap-resources", one, two);
  }

  @Test
  public void testOffheapSizeMismatch() {
    Cluster one = newDefaultCluster().setOffheapResource("first", 1, GB);
    Cluster two = newDefaultCluster().setOffheapResource("first", 100, GB);
    assertClusterValidationFails("Mismatch found in offheap-resources", one, two);
  }

  @Test
  public void testSecurityAuthcMismatch() {
    Cluster one = newDefaultCluster().setSecurityAuthc("certificate");
    Cluster two = newDefaultCluster().setSecurityAuthc("file");
    assertClusterValidationFails("Mismatch found in authc", one, two);
  }

  @Test
  public void testSecuritySslTlsMismatch() {
    Cluster one = newDefaultCluster();
    Cluster two = newDefaultCluster().setSecuritySslTls(true);
    assertClusterValidationFails("Mismatch found in ssl-tls", one, two);
  }

  @Test
  public void testSecurityWhitelistMismatch() {
    Cluster one = newDefaultCluster();
    Cluster two = newDefaultCluster().setSecurityWhitelist(true);
    assertClusterValidationFails("Mismatch found in whitelist", one, two);
  }

  @Test
  public void testNoMismatches() {
    Cluster one = newDefaultCluster().setFailoverPriority(availability()).setSecurityAuthc("file").setName("cluster");
    Cluster two = one.clone();
    new MutualClusterValidator(one, two).validate();
  }

  private void assertClusterValidationFails(String message, Cluster one, Cluster two) {
    exception.expect(ClusterConfigMismatchException.class);
    exception.expectMessage(message);
    new MutualClusterValidator(one, two).validate();
  }
}