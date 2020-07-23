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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.testing.TmpDir;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class FileConfigStorageTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Test
  public void saveAndRetrieve() throws Exception {
    Path root = temporaryFolder.getRoot();

    NodeContext topology = new NodeContext(Testing.newTestCluster("bar", new Stripe(Testing.newTestNode("node-1", "localhost"))), 1, "node-1");
    Properties properties = Props.load(new StringReader(new String(Files.readAllBytes(Paths.get(getClass().getResource("/config.properties").toURI())), StandardCharsets.UTF_8).replace("\\", "/")));

    FileConfigStorage storage = new FileConfigStorage(root, "node-1");

    assertFalse(Files.exists(root.resolve("node-1.1.properties")));
    storage.saveConfig(1L, topology);
    assertTrue(Files.exists(root.resolve("node-1.1.properties")));

    Properties written = Props.load(new StringReader(new String(Files.readAllBytes(root.resolve("node-1.1.properties")), StandardCharsets.UTF_8).replace("\\", "/")));
    assertThat(written.toString(), written, is(equalTo(properties)));

    NodeContext loaded = storage.getConfig(1L);
    assertThat(loaded, is(topology));
  }
}