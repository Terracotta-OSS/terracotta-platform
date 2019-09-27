/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.PathConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.terracottatech.dynamic_config.cli.common.FormatConverter;
import com.terracottatech.dynamic_config.cli.common.InetSocketAddressConverter;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.utilities.Json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@Parameters(commandNames = "export", commandDescription = "Export the cluster topology in a file or output to the console")
@Usage("export -s HOST[:PORT] [-d DESTINATION_DIRECTORY] [-f [json|properties]]")
public class ExportCommand extends RemoteCommand {

  public enum Format {JSON, PROPERTIES}

  @Parameter(names = {"-s"}, required = true, description = "Node to connect to for topology information", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-d"}, description = "Optional path of the folder when the topology information should be saved to. Defaults to the current working directory.", converter = PathConverter.class)
  private Path outputDir;

  @Parameter(names = {"-f"}, description = "Output format. Defaults to 'properties'.", converter = FormatConverter.class)
  private Format format;

  @Override
  public void validate() {
    if (outputDir == null) {
      outputDir = Paths.get(".");
    }
    if (format == null) {
      format = Format.PROPERTIES;
    }
  }

  @Override
  public final void run() {
    Cluster cluster = getRemoteTopology(node);
    String name = cluster.getName() == null ? "UNIDENTIFIED" : cluster.getName();
    if (outputDir == null) {
      String output = buildOutput(cluster, format);
      logger.info("Cluster '{}' from: {}\n{}", name, node, output);
    } else {
      try {
        Path file = outputDir.resolve(cluster.getName() + "." + format.name().toLowerCase());
        String output = buildOutput(cluster, format);
        Files.write(file, output.getBytes(StandardCharsets.UTF_8));
        logger.info("Output saved to: {}", file);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private String buildOutput(Cluster cluster, Format format) {
    switch (format) {
      case JSON:
        try {
          // shows optional values that are unset
          return Json.copyObjectMapper(true)
              .setSerializationInclusion(JsonInclude.Include.ALWAYS)
              .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
              .writeValueAsString(cluster);
        } catch (JsonProcessingException e) {
          throw new AssertionError(format);
        }
      case PROPERTIES:
        Properties properties = cluster.toProperties();
        try (StringWriter writer = new StringWriter()) {
          properties.store(writer, "");
          return writer.toString();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      default:
        throw new AssertionError(format);
    }
  }
}
