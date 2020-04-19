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
package org.terracotta.dynamic_config.test_support.util;

import org.terracotta.dynamic_config.cli.config_converter.ConfigConverter;
import org.terracotta.dynamic_config.cli.config_converter.ConfigRepoProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static org.junit.Assert.assertFalse;

/**
 * @author Mathieu Carbou
 */
public class ConfigRepositoryGenerator {

  public interface PortSupplier {
    int getNodePort(int stripeId, int nodeId);

    int getNodeGroupPort(int stripeId, int nodeId);

    static PortSupplier fromList(int nodesPerStripe, int... ports) {
      return new PortSupplier() {
        @Override
        public int getNodePort(int stripeId, int nodeId) {
          return ports[2 * (nodeId - 1) + 2 * nodesPerStripe * (stripeId - 1)];
        }

        @Override
        public int getNodeGroupPort(int stripeId, int nodeId) {
          return getNodePort(stripeId, nodeId);
        }
      };
    }
  }

  private final Path root;
  private final PortSupplier portSupplier;

  public ConfigRepositoryGenerator(Path root, PortSupplier portSupplier) {
    this.root = root;
    this.portSupplier = portSupplier;
  }

  public ConfigRepositoryGenerator(Path root, int nodesPerStripe, int... ports) {
    this(root, PortSupplier.fromList(nodesPerStripe, ports));
  }

  public void generate2Stripes2Nodes() {
    convert(substituteParams(1, 2, "/tc-configs/stripe1-2-nodes.xml"), substituteParams(2, 2, "/tc-configs/stripe2-2-nodes.xml"));
  }

  public void generate1Stripe2Nodes() {
    convert(substituteParams(1, 2, "/tc-configs/stripe1-2-nodes.xml"));
  }

  public void generate1Stripe1NodeIpv6() {
    convert(substituteParams(1, 1, "/tc-configs/stripe1-1-node_ipv6.xml"));
  }

  public void generate1Stripe1Node() {
    convert(substituteParams(1, 1, "/tc-configs/stripe1-1-node.xml"));
  }

  public void generate1Stripe1NodeAndSkipCommit() {
    convert(true, substituteParams(1, 1, "/tc-configs/stripe1-1-node.xml"));
  }

  private Path substituteParams(int stripeId, int nodes, String path) {
    String defaultConfig;
    try {
      defaultConfig = String.join(System.lineSeparator(), Files.readAllLines(Paths.get(ConfigRepositoryGenerator.class.getResource(path).toURI())));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    String configuration = defaultConfig;
    for (int i = 1; i <= nodes; i++) {
      configuration = configuration
          .replace("${PORT-" + i + "}", String.valueOf(portSupplier.getNodePort(stripeId, i)))
          .replace("${GROUP-PORT-" + i + "}", String.valueOf(portSupplier.getNodeGroupPort(stripeId, i)));
    }

    try {
      Path temporaryTcConfigXml = Files.createTempFile("tc-config-tmp.", ".xml");
      return Files.write(temporaryTcConfigXml, configuration.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void convert(Path... tcConfigPaths) {
    convert(false, tcConfigPaths);
  }

  private void convert(boolean skipCommit, Path... tcConfigPaths) {
    try {
      assertFalse("Directory already exists: " + root, Files.exists(root));
      createDirectories(root);
      ConfigRepoProcessor resultProcessor = skipCommit ? new CommitSkippingConfigRepoProcessor(root) : new ConfigRepoProcessor(root);
      ConfigConverter converter = new ConfigConverter(resultProcessor::process);
      converter.processInput("testCluster", tcConfigPaths);

      URL licenseUrl = ConfigRepositoryGenerator.class.getResource("/license.xml");
      if (licenseUrl != null) {
        Path licensePath = Paths.get(licenseUrl.toURI());
        try (Stream<Path> pathList = Files.list(root)) {
          pathList.forEach(repoPath -> {
            try {
              copy(licensePath, createDirectories(repoPath.resolve("license")).resolve(licensePath.getFileName()));
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
        }
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    new ConfigRepositoryGenerator(Paths.get("target/test-data/repos/single-stripe-single-node"), 1, 9410, 9430).generate1Stripe1Node();
    new ConfigRepositoryGenerator(Paths.get("target/test-data/repos/single-stripe-multi-node"), 2, 9410, 9430, 9510, 9530).generate1Stripe2Nodes();
    new ConfigRepositoryGenerator(Paths.get("target/test-data/repos/multi-stripe"), 2, 9410, 9430, 9510, 9530, 9610, 9630, 9710, 9730).generate2Stripes2Nodes();
  }
}
