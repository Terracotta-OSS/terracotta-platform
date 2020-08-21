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
import java.util.Properties;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

@ClusterDefinition(stripes = 2)
public class ImportCommand2x1IT extends DynamicConfigIT {
  @Test
  public void test_import() throws Exception {
    getUpcomingCluster("localhost", getNodePort()).toProperties(false, true, true);

    Path configFile = copyConfigProperty("/config-property-files/import2x1.properties");
    invokeConfigTool("import", "-f", configFile.toString());

    Properties after = getUpcomingCluster("localhost", getNodePort()).toProperties(false, true, true);

    Properties expected = Props.load(configFile);
    Stream.of("cluster-uid",
        "stripe.1.stripe-uid",
        "stripe.2.stripe-uid",
        "stripe.1.node.1.node-uid",
        "stripe.2.node.1.node-uid"
    ).forEach(prop -> expected.put(prop, after.get(prop)));

    assertThat("EXPECTED:\n" + Props.toString(expected) + "\nAFTER\n" + Props.toString(after), after, is(equalTo(expected)));
  }
}
