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

import com.terracotta.connection.api.TerracottaConnectionService;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.dynamic_config.entity.topology.client.DynamicTopologyEntity;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.net.URI;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(autoActivate = true)
public class DynamicTopologyEntityIT extends DynamicConfigIT {
  @Test
  public void test_topology_entity() throws Exception {
    try (Connection connection = new TerracottaConnectionService().connect(URI.create("terracotta://localhost:" + getNodePort()), new Properties())) {
      EntityRef<DynamicTopologyEntity, Void, Void> ref = connection.getEntityRef(DynamicTopologyEntity.class, 1L, "dynamic-config-topology-entity");
      DynamicTopologyEntity entity = ref.fetchEntity(null);

      assertThat(entity.getUpcomingCluster(), is(equalTo(getUpcomingCluster(1, 1))));
      assertThat(entity.getRuntimeCluster(), is(equalTo(getRuntimeCluster(1, 1))));
      assertThat(entity.getLicense(), is(nullValue()));
    }
  }
}
