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
import org.terracotta.dynamic_config.cli.config_converter.ConfigConverterTool;
import org.terracotta.dynamic_config.test_support.ClusterDefinition;
import org.terracotta.dynamic_config.test_support.DynamicConfigIT;
import org.terracotta.dynamic_config.test_support.util.ConfigurationGenerator;

import java.nio.file.Path;

import static org.hamcrest.Matchers.is;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;

@ClusterDefinition(autoStart = false)
public class ConfigConversionStartupIT extends DynamicConfigIT {
  @Test
  public void testStartupAfterConfigConversionForDefaultFailoverLeaseAndClientReconnWindow() throws Exception {
    Path repositoriesDir = getBaseDir().resolve("generated-configs");
    ConfigurationGenerator configGenerator = getConfigGenerator(repositoriesDir);
    Path tcConfig = configGenerator.substituteParams(1, 1, "/conversion/tc-config_missing_failover_lease_client_reconnect.xml");
    ConfigConverterTool.start("convert",
        "-c", tcConfig.toString(),
        "-n", "cluster_default_lease_failover_reconnect_window",
        "-d", repositoriesDir.toAbsolutePath().toString(),
        "-f");

    Path nodeRepositoryDir = getBaseDir().resolve(getNodeConfigDir(1, 1));
    org.terracotta.utilities.io.Files.copy(repositoriesDir.resolve("stripe-1").resolve("node-1"), nodeRepositoryDir, RECURSIVE);
    startNode(1, 1, "-r", repositoriesDir.resolve("stripe-1").resolve("node-1").toString());
    waitUntil(() -> angela.tsa().getActives().size(), is(1));
  }
}
