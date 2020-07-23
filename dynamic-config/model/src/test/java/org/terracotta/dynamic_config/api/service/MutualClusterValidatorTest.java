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
import org.terracotta.dynamic_config.api.model.Testing;

import static org.terracotta.common.struct.MemoryUnit.GB;
import static org.terracotta.common.struct.TimeUnit.SECONDS;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.availability;
import static org.terracotta.dynamic_config.api.model.FailoverPriority.consistency;

public class MutualClusterValidatorTest {
  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Test
  public void testClusterNameMismatch() {
    Cluster one = new Cluster().setName("one");
    Cluster two = new Cluster().setName("two");
    assertClusterValidationFails("Mismatch found in cluster-name", one, two);
  }

  @Test
  public void testClientLeaseDurationMismatch() {
    Cluster one = new Cluster().setClientLeaseDuration(1, SECONDS);
    Cluster two = new Cluster().setClientLeaseDuration(100, SECONDS);
    assertClusterValidationFails("Mismatch found in client-lease-duration", one, two);
  }

  @Test
  public void testClientReconnectWindowMismatch() {
    Cluster one = new Cluster().setClientReconnectWindow(1, SECONDS);
    Cluster two = new Cluster().setClientReconnectWindow(100, SECONDS);
    assertClusterValidationFails("Mismatch found in client-reconnect", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_availabilityVsConsistency() {
    Cluster one = new Cluster().setFailoverPriority(availability());
    Cluster two = new Cluster().setFailoverPriority(consistency());
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_availabilityVsConsistencyVoter() {
    Cluster one = new Cluster().setFailoverPriority(availability());
    Cluster two = new Cluster().setFailoverPriority(consistency(1));
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testFailoverPriorityMismatch_consistencyVsConsistencyVoter() {
    Cluster one = new Cluster().setFailoverPriority(consistency());
    Cluster two = new Cluster().setFailoverPriority(consistency(1));
    assertClusterValidationFails("Mismatch found in failover-priority", one, two);
  }

  @Test
  public void testOffheapNameMismatch() {
    Cluster one = new Cluster().setOffheapResource("first", 1, GB);
    Cluster two = new Cluster().setOffheapResource("second", 1, GB);
    assertClusterValidationFails("Mismatch found in offheap-resources", one, two);
  }

  @Test
  public void testOffheapSizeMismatch() {
    Cluster one = new Cluster().setOffheapResource("first", 1, GB);
    Cluster two = new Cluster().setOffheapResource("first", 100, GB);
    assertClusterValidationFails("Mismatch found in offheap-resources", one, two);
  }

  @Test
  public void testSecurityAuthcMismatch() {
    Cluster one = new Cluster().setSecurityAuthc("certificate");
    Cluster two = new Cluster().setSecurityAuthc("file");
    assertClusterValidationFails("Mismatch found in authc", one, two);
  }

  @Test
  public void testSecuritySslTlsMismatch() {
    Cluster one = Testing.newTestCluster();
    Cluster two = Testing.newTestCluster().setSecuritySslTls(true);
    assertClusterValidationFails("Mismatch found in ssl-tls", one, two);
  }

  @Test
  public void testSecurityWhitelistMismatch() {
    Cluster one = Testing.newTestCluster();
    Cluster two = Testing.newTestCluster().setSecurityWhitelist(true);
    assertClusterValidationFails("Mismatch found in whitelist", one, two);
  }

  @Test
  public void testNoMismatches() {
    Cluster one = Testing.newTestCluster().setSecurityAuthc("file").setName("cluster");
    Cluster two = one.clone();
    new MutualClusterValidator(one, two).validate();
  }

  private void assertClusterValidationFails(String message, Cluster one, Cluster two) {
    exception.expect(ClusterConfigMismatchException.class);
    exception.expectMessage(message);
    new MutualClusterValidator(one, two).validate();
  }
}