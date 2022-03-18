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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Rule;
import org.junit.Test;
import org.terracotta.dynamic_config.api.json.DynamicConfigApiJsonModule;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.DynamicConfigNomadChange;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.server.configuration.nomad.NomadServerFactory;
import org.terracotta.json.ObjectMapperFactory;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.UpgradableNomadServer;
import org.terracotta.testing.TmpDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.terracotta.utilities.io.Files.ExtendedOption.RECURSIVE;

public class BackwardCompatibilityTest {

  @Rule
  public TmpDir temporaryFolder = new TmpDir(Paths.get(System.getProperty("user.dir"), "target"), false);

  @Test
  public void test_automatic_upgrade_of_config_repository() throws Exception {
    Path resourcesRoot = Paths.get(getClass().getResource("/config-v1").toURI());

    // copy config folder in a temporary location
    Path config = temporaryFolder.getRoot().resolve("config-v1");
    org.terracotta.utilities.io.Files.copy(resourcesRoot, config, RECURSIVE);
    Files.createDirectories(config.resolve("license"));

    // before...
    Properties before = Props.load(config.resolve("cluster").resolve("default-node1.1.properties"));
    assertThat(before.stringPropertyNames(), not(hasItem("stripe.1.stripe-name")));

    // create nomad server
    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(config, IParameterSubstitutor.identity());
    nomadConfigurationManager.createDirectories();
    ObjectMapperFactory objectMapperFactory = new ObjectMapperFactory().withModule(new DynamicConfigApiJsonModule());
    NomadServerFactory nomadServerFactory = new NomadServerFactory(objectMapperFactory);

    try (UpgradableNomadServer<NodeContext> nomadServer = nomadServerFactory.createServer(
        nomadConfigurationManager,
        ChangeApplicator.allow((nodeContext, change) -> new NodeContext(((DynamicConfigNomadChange) change).apply(nodeContext.getCluster()), 1, "default-node1")),
        "default-node1",
        null)) {

      // upgrade should have been done
      Properties after = Props.load(config.resolve("cluster").resolve("default-node1.2.properties"));
      assertThat(after.stringPropertyNames(), hasItem("stripe.1.stripe-name"));
      assertThat(after.stringPropertyNames(), hasItem("this.version"));

      // check content should match v1 content plus these 2 fields
      before.setProperty("stripe.1.stripe-name", after.getProperty("stripe.1.stripe-name"));
      before.setProperty("this.version", "2");
      assertThat(after, is(equalTo(before)));

      // check topology
      NodeContext topology = nomadServer.discover().getLatestChange().getResult();
      assertThat(topology.getCluster().getSingleStripe().get().getName(), is(equalTo("stripe[0]")));

      // subsequent calls are outputting the same result always after an upgrade
      assertThat(nomadServer.discover().getLatestChange().getResult(), is(equalTo(topology)));

      ObjectMapper objectMapper = objectMapperFactory.create();
      assertThat(
          objectMapper.writeValueAsString(topology),
          objectMapper.valueToTree(topology),
          is(equalTo(objectMapper.readTree(read("/topology.json")))));
    }
  }

  private String read(String resource) throws URISyntaxException, IOException {
    URL url = getClass().getResource(resource);
    if (url == null) {
      throw new AssertionError(resource);
    }
    Path path = Paths.get(url.toURI());
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }
}