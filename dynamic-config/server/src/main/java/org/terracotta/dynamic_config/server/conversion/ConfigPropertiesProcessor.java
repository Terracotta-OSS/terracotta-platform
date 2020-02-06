/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.conversion;

import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Props;
import org.w3c.dom.Node;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;

public class ConfigPropertiesProcessor extends PostConversionProcessor {
  private final Path outputDir;
  private final String clusterName;

  public ConfigPropertiesProcessor(Path outputDir, String clusterName) {
    this.outputDir = outputDir;
    this.clusterName = clusterName;
  }

  @Override
  public void process(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap) {
    process(nodeNameNodeConfigMap, false);
  }

  @Override
  public void process(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap, boolean acceptRelativePaths) {
    ArrayList<NodeContext> nodeContexts = validate(nodeNameNodeConfigMap, acceptRelativePaths);
    Properties properties = nodeContexts.get(0).getCluster().setName(clusterName).toProperties();
    try (StringWriter out = new StringWriter()) {
      Props.store(out, properties, "Converted cluster configuration:");
      Files.createDirectories(outputDir);
      Files.write(outputDir.resolve(clusterName + ".properties"), out.toString().getBytes(UTF_8), CREATE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}