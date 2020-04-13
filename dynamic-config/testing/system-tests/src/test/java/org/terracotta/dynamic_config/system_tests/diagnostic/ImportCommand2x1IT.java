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

import org.junit.Test;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;

import java.nio.file.Path;
import java.util.TreeMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.terracotta.dynamic_config.test_support.util.AngelaMatchers.successful;

@ClusterDefinition(stripes = 2)
public class ImportCommand2x1IT extends DynamicConfigIT {
  @Test
  public void test_import() throws Exception {
    TreeMap<Object, Object> before = new TreeMap<>(getUpcomingCluster("localhost", getNodePort()).toProperties(false, true));
    Path path = copyConfigProperty("/config-property-files/import2x1.properties");
    assertThat(
        configToolInvocation("import", "-f", path.toString()),
        is(successful()));
    TreeMap<Object, Object> after = new TreeMap<>(getUpcomingCluster("localhost", getNodePort()).toProperties(false, true));
    TreeMap<Object, Object> expected = new TreeMap<>(Props.load(path));
    assertThat(after, is(equalTo(expected)));
    assertThat(before, is(not(equalTo(expected))));
  }
}
