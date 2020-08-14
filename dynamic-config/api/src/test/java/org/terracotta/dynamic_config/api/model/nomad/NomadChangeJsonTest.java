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
package org.terracotta.dynamic_config.api.model.nomad;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.client.change.NomadChange;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_BACKUP_DIR;
import static org.terracotta.dynamic_config.api.model.Setting.OFFHEAP_RESOURCES;

/**
 * @author Mathieu Carbou
 */
public class NomadChangeJsonTest {

  ObjectMapper objectMapper = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule()).create();

  private final Cluster cluster = Testing.newTestCluster("myClusterName", new Stripe().addNode(Testing.newTestNode("foo", "localhost", 9410)))
      .setClientReconnectWindow(60, TimeUnit.SECONDS)
      .putOffheapResource("foo", 1, MemoryUnit.GB);

  @Test
  public void test_ser_deser() throws IOException, URISyntaxException {
    NomadChange[] changes = {
        new ClusterActivationNomadChange(cluster),
        SettingNomadChange.set(Applicability.node(1, "node1"), NODE_BACKUP_DIR, "backup"),
        new MultiSettingNomadChange(
            SettingNomadChange.set(Applicability.node(1, "node1"), NODE_BACKUP_DIR, "backup"),
            SettingNomadChange.set(Applicability.cluster(), OFFHEAP_RESOURCES, "bar", "512MB")
        ),
        new FormatUpgradeNomadChange(Version.V1, Version.V2)
    };

    for (int i = 0; i < changes.length; i++) {
      NomadChange change = changes[i];

      URL jsonFile = getClass().getResource("/nomad/change" + i + ".json");
      byte[] bytes = Files.readAllBytes(Paths.get(jsonFile.toURI()));
      String json = new String(bytes, StandardCharsets.UTF_8);

      if (isWindows()) {
        json = json.replace("/", "\\\\");
      }

      assertThat(jsonFile.getPath(), objectMapper.valueToTree(change).toString(), is(equalTo(objectMapper.readTree(json).toString())));
      assertThat(jsonFile.getPath(), objectMapper.readValue(json, NomadChange.class), is(equalTo(change)));
    }
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().startsWith("windows");
  }
}
