/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import com.terracottatech.migration.MigrationImpl;
import com.terracottatech.migration.nomad.RepositoryStructureBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

import static java.nio.file.Files.copy;
import static java.nio.file.Files.createDirectories;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertFalse;

/**
 * @author Mathieu Carbou
 */
public class ConfigRepositoryGenerator {

  private final Path root;
  private int inUse;
  private int[] ports;

  public ConfigRepositoryGenerator(Path root, int... ports) {
    this.root = root;
    this.ports = ports;
  }

  public void generate2Stripes2Nodes() {
    migrate(
        asList(
            "1," + substituteParams(2, "/tc-configs/stripe1-2-nodes.xml"),
            "2," + substituteParams(2, "/tc-configs/stripe2-2-nodes.xml")
        )
    );
  }

  public void generate1Stripe2Nodes() {
    migrate(singletonList("1," + substituteParams(2, "/tc-configs/stripe1-2-nodes.xml")));
  }

  public void generate1Stripe1NodeIpv6() {
    migrate(singletonList("1," + substituteParams(1, "/tc-configs/stripe1-1-node_ipv6.xml")));
  }

  public void generate1Stripe1Node() {
    migrate(singletonList("1," + substituteParams(1, "/tc-configs/stripe1-1-node.xml")));
  }

  public void generate1Stripe1NodeAndSkipCommit() {
    migrate(singletonList("1," + substituteParams(1, "/tc-configs/stripe1-1-node.xml")), true);
  }

  private Path substituteParams(int nodes, String s) {
    int portsNeeded = 2 * nodes; // one for port and group-port each
    if (ports.length - inUse < portsNeeded) {
      throw new IllegalArgumentException("Not enough ports to use. Required: " + portsNeeded + ", found: " + (ports.length - inUse));
    }

    String defaultConfig;
    try {
      defaultConfig = String.join(System.lineSeparator(), Files.readAllLines(Paths.get(ConfigRepositoryGenerator.class.getResource(s).toURI())));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }

    String configuration = defaultConfig;
    for (int i = 1; i <= nodes; i++) {
      configuration = configuration
          .replace("${PORT-" + i + "}", String.valueOf(ports[inUse++]))
          .replace("${GROUP-PORT-" + i + "}", String.valueOf(ports[inUse++]));
    }

    try {
      Path temporaryTcConfigXml = Files.createTempFile("tc-config-tmp.", ".xml");
      return Files.write(temporaryTcConfigXml, configuration.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void migrate(List<String> migrationStrings) {
    migrate(migrationStrings, false);
  }

  private void migrate(List<String> migrationStrings, boolean skipCommit) {
    try {
      assertFalse("Directory already exists: " + root, Files.exists(root));
      createDirectories(root);
      RepositoryStructureBuilder resultProcessor =
          skipCommit ? new CommitSkippingRepositoryBuilder(root) : new RepositoryStructureBuilder(root);
      MigrationImpl migration = new MigrationImpl(resultProcessor::process);
      migration.processInput("testCluster", migrationStrings);

      Path license = Paths.get(ConfigRepositoryGenerator.class.getResource("/license.xml").toURI());
      try (Stream<Path> pathList = Files.list(root)) {
        pathList.forEach(repoPath -> {
          try {
            copy(license, createDirectories(repoPath.resolve("license")).resolve(license.getFileName()));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) {
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/single-stripe-single-node"), 9410, 9430).generate1Stripe1Node();
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/single-stripe-multi-node"), 9410, 9430, 9510, 9530).generate1Stripe2Nodes();
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/multi-stripe"), 9410, 9430, 9510, 9530, 9610, 9630, 9710, 9730).generate2Stripes2Nodes();
  }
}
