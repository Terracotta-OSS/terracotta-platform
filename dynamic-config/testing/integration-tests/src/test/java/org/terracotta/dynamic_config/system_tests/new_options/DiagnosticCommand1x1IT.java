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
package org.terracotta.dynamic_config.system_tests.new_options;

import org.junit.Test;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Files;
import java.nio.file.Paths;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.terracotta.angela.client.support.hamcrest.AngelaMatchers.containsLinesInOrderStartingWith;

@ClusterDefinition(autoStart = false)
public class DiagnosticCommand1x1IT extends DynamicConfigIT {
  @Test
  public void test_diagnostic_on_unconfigured_node() throws Exception {
    startNode(1, 1);
    assertThat(
        configTool("diagnostic", "-connect-to", "localhost:" + getNodePort(1, 1)),
        containsLinesInOrderStartingWith(Files.lines(Paths.get(getClass().getResource("/diagnostic-output/diagnostic1.txt").toURI())).collect(toList())));
  }
}
