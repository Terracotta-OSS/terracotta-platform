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
package org.terracotta.dynamic_config.system_tests.diagnostic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Path;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.successful;

/**
 * @author Mathieu Carbou
 */
@ClusterDefinition(stripes = 2, nodesPerStripe = 2, failoverPriority = "")
public class AttachDetachCommand2x2IT extends DynamicConfigIT {
  private static final String OUTPUT_JSON_FILE = "attach-detach-output.json";

  @Test
  public void test_attach_detach_with_unconfigured_nodes() throws Exception {
    Path file = tmpDir.getRoot().resolve(OUTPUT_JSON_FILE);

    assertThat(configTool("export", "-s", "localhost:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));

    ObjectMapper objectMapper = objectMapperFactory.create();

    Cluster cluster = objectMapper.readValue(file.toFile(), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodes(), hasSize(1));

    // add a node
    assertThat(configTool("attach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));

    cluster = objectMapper.readValue(file.toFile(), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodes(), hasSize(2));

    // add a stripe
    assertThat(configTool("attach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));

    cluster = objectMapper.readValue(file.toFile(), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(2));
    assertThat(cluster.getNodes(), hasSize(3));

    // remove the previously added stripe
    assertThat(configTool("detach", "-t", "stripe", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(2, 1)), is(successful()));
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));

    cluster = objectMapper.readValue(file.toFile(), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodes(), hasSize(2));

    // remove the previously added node
    assertThat(configTool("detach", "-d", "localhost:" + getNodePort(), "-s", "localhost:" + getNodePort(1, 2)), is(successful()));
    assertThat(configTool("export", "-s", "localhost:" + getNodePort(), "-f", file.toString(), "-t", "json"), is(successful()));

    cluster = objectMapper.readValue(file.toFile(), Cluster.class);
    assertThat(cluster.getStripes(), hasSize(1));
    assertThat(cluster.getNodes(), hasSize(1));
  }
}
