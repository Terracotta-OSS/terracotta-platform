/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.conversion;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Props;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;

public class ConfigPropertiesProcessor implements PostConversionProcessor {
  private final Path outputDir;
  private final String clusterName;

  public ConfigPropertiesProcessor(Path outputDir, String clusterName) {
    this.outputDir = outputDir;
    this.clusterName = clusterName;
  }

  @Override
  public void process(Cluster cluster) {
    Properties properties = cluster.toProperties();
    try (StringWriter out = new StringWriter()) {
      Props.store(out, properties, "Converted cluster configuration:");
      Files.createDirectories(outputDir);
      Files.write(outputDir.resolve(clusterName + ".properties"), out.toString().getBytes(UTF_8), CREATE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}