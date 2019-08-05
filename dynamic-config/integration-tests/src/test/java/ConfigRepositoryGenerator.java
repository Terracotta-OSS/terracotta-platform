/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import com.terracottatech.dynamic_config.test.util.MigrationITResultProcessor;
import com.terracottatech.migration.MigrationImpl;
import com.terracottatech.nomad.server.NomadServer;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static java.util.Comparator.reverseOrder;

/**
 * @author Mathieu Carbou
 */
public class ConfigRepositoryGenerator {
  public static void main(String[] args) throws URISyntaxException, IOException {
    File root = new File("build/test-data/repos");

    multi_stripe(root);
    single_stripe_multi_node(root);
    single_stripe_single_node(root);

    Path license = Paths.get(ConfigRepositoryGenerator.class.getResource("/license.xml").toURI());
    Files.walk(root.toPath())
        .filter(path -> path.getFileName().toString().equals("license"))
        .forEach(path -> {
          try {
            Files.copy(license, path.resolve(license.getFileName()));
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        });
  }

  private static void multi_stripe(File root) throws IOException, URISyntaxException {
    File output = new File(root, "multi-stripe");
    if (output.exists()) {
      Files.walk(output.toPath()).sorted(reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    output.mkdirs();

    Map<String, NomadServer<String>> serverMap = new HashMap<>();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(output.toPath(), serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path stripe1 = Paths.get(ConfigRepositoryGenerator.class.getResource("/config-repositories/multi-stripe/stripe1.xml").toURI());
    Path stripe2 = Paths.get(ConfigRepositoryGenerator.class.getResource("/config-repositories/multi-stripe/stripe2.xml").toURI());

    migration.processInput("testCluster", Arrays.asList("1," + stripe1, "2," + stripe2));
  }

  private static void single_stripe_multi_node(File root) throws IOException, URISyntaxException {
    File output = new File(root, "single-stripe-multi-node");
    if (output.exists()) {
      Files.walk(output.toPath()).sorted(reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    output.mkdirs();

    Map<String, NomadServer<String>> serverMap = new HashMap<>();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(output.toPath(), serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path stripe1 = Paths.get(ConfigRepositoryGenerator.class.getResource("/config-repositories/single-stripe-multi-node/stripe1.xml").toURI());

    migration.processInput("testCluster", Collections.singletonList("1," + stripe1));
  }

  private static void single_stripe_single_node(File root) throws IOException, URISyntaxException {
    File output = new File(root, "single-stripe-single-node");
    if (output.exists()) {
      Files.walk(output.toPath()).sorted(reverseOrder()).map(Path::toFile).forEach(File::delete);
    }
    output.mkdirs();

    Map<String, NomadServer<String>> serverMap = new HashMap<>();
    MigrationITResultProcessor resultProcessor = new MigrationITResultProcessor(output.toPath(), serverMap);
    MigrationImpl migration = new MigrationImpl(resultProcessor);

    Path stripe1 = Paths.get(ConfigRepositoryGenerator.class.getResource("/config-repositories/single-stripe-single-node/stripe1.xml").toURI());

    migration.processInput("testCluster", Collections.singletonList("1," + stripe1));
  }
}
