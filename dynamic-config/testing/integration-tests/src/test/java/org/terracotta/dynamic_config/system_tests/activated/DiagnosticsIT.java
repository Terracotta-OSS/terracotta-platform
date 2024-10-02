/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import com.terracotta.diagnostic.Diagnostics;
import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.connection.ConnectionFactory;
import org.terracotta.connection.ConnectionPropertyNames;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.util.Properties;

import static java.util.Collections.singletonList;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

@ClusterDefinition(autoActivate = true, failoverPriority = "")
public class DiagnosticsIT extends DynamicConfigIT {
  @Test
  public void testGetConfigByAddingOffheapResource() throws Exception {
    final String newOffheapName = "new-test-offheap-resource";

    Properties properties = new Properties();
    properties.setProperty(ConnectionPropertyNames.CONNECTION_TYPE, "diagnostic");

    try (Connection connection =
             ConnectionFactory.connect(singletonList(getNodeAddress(1, 1)), properties)) {
      EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
      Diagnostics diagnostics = ref.fetchEntity(null);
      assertThat(diagnostics.getConfig(), not(containsString(newOffheapName)));
    }

    assertThat(configTool("set", "-s", "localhost:" + getNodePort(), "-c", "offheap-resources=" + newOffheapName + ":1GB"), is(successful()));

    try (Connection connection =
             ConnectionFactory.connect(singletonList(getNodeAddress(1, 1)), properties)) {
      EntityRef<Diagnostics, Object, Void> ref = connection.getEntityRef(Diagnostics.class, 1, "root");
      Diagnostics diagnostics = ref.fetchEntity(null);
      assertThat(diagnostics.getConfig(), containsString(newOffheapName));
    }
  }
}
