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
package org.terracotta.lease.service;

import org.junit.Test;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.nomad.SettingNomadChange;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.lease.service.config.LeaseConfiguration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.nomad.Applicability.cluster;

public class LeaseConfigChangeHandlerTest {

  private NodeContext topology = new NodeContext(Cluster.newDefaultCluster("foo", new Stripe(Node.newDefaultNode("bar", "localhost"))), 1, "bar");
  private SettingNomadChange set = SettingNomadChange.set(cluster(), Setting.CLIENT_LEASE_DURATION, "20s");

  @Test
  public void testTryApply() throws InvalidConfigChangeException {
    LeaseConfiguration leaseConfiguration = new LeaseConfiguration(1000L);
    LeaseConfigChangeHandler leaseConfigChangeHandler = new LeaseConfigChangeHandler(leaseConfiguration);
    leaseConfigChangeHandler.validate(topology, set.toConfiguration(topology.getCluster()));
    assertThat(set.apply(topology.getCluster()).getClientLeaseDuration().getQuantity(TimeUnit.SECONDS), is(20L));
  }

  @Test
  public void testApplyChange() {
    LeaseConfiguration leaseConfiguration = new LeaseConfiguration(1000L);
    LeaseConfigChangeHandler leaseConfigChangeHandler = new LeaseConfigChangeHandler(leaseConfiguration);
    leaseConfigChangeHandler.apply(set.toConfiguration(topology.getCluster()));
    assertThat(leaseConfiguration.getLeaseLength(), is(20000L));
  }
}
