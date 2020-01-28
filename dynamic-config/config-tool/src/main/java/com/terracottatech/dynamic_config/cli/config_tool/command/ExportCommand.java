/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.BooleanConverter;
import com.beust.jcommander.converters.PathConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.terracottatech.dynamic_config.api.model.Cluster;
import com.terracottatech.dynamic_config.api.model.Props;
import com.terracottatech.dynamic_config.cli.command.Usage;
import com.terracottatech.dynamic_config.cli.config_tool.converter.OutputFormat;
import com.terracottatech.dynamic_config.cli.converter.InetSocketAddressConverter;
import com.terracottatech.json.Json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.lang.System.lineSeparator;

@Parameters(commandNames = "export", commandDescription = "Export a cluster configuration")
@Usage("export -s <hostname[:port]> [-f <config-file>] [-x] [-r]")
public class ExportCommand extends RemoteCommand {
  @Parameter(names = {"-s"}, required = true, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Output configuration file", converter = PathConverter.class)
  private Path outputFile;

  @Parameter(names = {"-x"}, description = "Exclude default values. Default: false", converter = BooleanConverter.class)
  private boolean excludeDefaultValues;

  @Parameter(names = {"-r"}, description = "Export the runtime configuration instead of the configuration saved on disk. Default: false", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  @Parameter(names = {"-t"}, hidden = true, description = "Output type (properties|json). Default: properties", converter = OutputFormat.FormatConverter.class)
  private OutputFormat outputFormat = OutputFormat.PROPERTIES;

  @Override
  public void validate() {
    if (outputFile != null && Files.exists(outputFile) && !Files.isRegularFile(outputFile)) {
      throw new IllegalArgumentException(outputFile + " is not a file");
    }
    validateAddress(node);
  }

  @Override
  public final void run() {
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    String output = buildOutput(cluster, outputFormat);

    if (outputFile == null) {
      logger.info("{}", output);

    } else {
      try {
        if (Files.exists(outputFile)) {
          logger.warn(outputFile + " already exists. Replacing this file.");
        } else {
          // try to create the parent directories
          Path dir = outputFile.toAbsolutePath().getParent();
          if (dir != null) {
            Files.createDirectories(dir);
          }
        }
        Files.write(outputFile, output.getBytes(StandardCharsets.UTF_8));
        logger.info("Output saved to: {}" + lineSeparator(), outputFile);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private String buildOutput(Cluster cluster, OutputFormat outputFormat) {
    switch (outputFormat) {
      case JSON:
        try {
          // shows optional values that are unset
          return Json.copyObjectMapper(true)
              .setSerializationInclusion(JsonInclude.Include.ALWAYS)
              .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
              .writeValueAsString(cluster);
        } catch (JsonProcessingException e) {
          throw new AssertionError(outputFormat);
        }
      case PROPERTIES:
        Properties nonDefaults = cluster.toProperties(false, false);
        try (StringWriter out = new StringWriter()) {
          Props.store(out, nonDefaults, "Non-default configurations:");
          if (!this.excludeDefaultValues) {
            Properties defaults = cluster.toProperties(false, true);
            defaults.keySet().removeAll(nonDefaults.keySet());
            out.write(System.lineSeparator());
            Props.store(out, defaults, "Default configurations:");
          }
          return out.toString();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      default:
        throw new AssertionError(outputFormat);
    }
  }
}
