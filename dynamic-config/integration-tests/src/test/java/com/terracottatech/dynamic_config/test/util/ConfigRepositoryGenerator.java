package com.terracottatech.dynamic_config.test.util;/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import com.terracottatech.migration.MigrationImpl;
import com.terracottatech.migration.nomad.RepositoryStructureBuilder;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

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

  public ConfigRepositoryGenerator(Path root) {
    this.root = root;
  }

  public void generate2Stripes2Nodes() {
    try {
      migrate(asList(
          "1," + Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/stripe1-2-nodes.xml").toURI()),
          "2," + Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/stripe2-2-nodes.xml").toURI())));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void generate1Stripe2Nodes() {
    try {
      migrate(singletonList(
          "1," + Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/stripe1-2-nodes.xml").toURI())));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void generate1Stripe1Node() {
    try {
      migrate(singletonList(
          "1," + Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/stripe1-1-node.xml").toURI())));
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public void generate1Stripe1NodeAndSkipCommit() {
    try {
      migrate(singletonList(
          "1," + Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/stripe1-1-node.xml").toURI())), true);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
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

      Path license = Paths.get(ConfigRepositoryGenerator.class.getResource("/tc-configs/license.xml").toURI());
      Files.list(root)
          .forEach(repoPath -> {
            try {
              copy(license, createDirectories(repoPath.resolve("license")).resolve(license.getFileName()));
            } catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws Exception {
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/single-stripe-single-node")).generate1Stripe1Node();
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/single-stripe-multi-node")).generate1Stripe2Nodes();
    new ConfigRepositoryGenerator(Paths.get("build/test-data/repos/multi-stripe")).generate2Stripes2Nodes();
  }
}
