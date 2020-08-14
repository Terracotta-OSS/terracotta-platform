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
package org.terracotta.dynamic_config.cli.config_tool.command;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.converters.BooleanConverter;
import com.beust.jcommander.converters.PathConverter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.cli.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.command.Usage;
import org.terracotta.dynamic_config.cli.config_tool.converter.OutputFormat;
import org.terracotta.dynamic_config.cli.converter.InetSocketAddressConverter;
import org.terracotta.json.ObjectMapperFactory;

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
@Usage("export -s <hostname[:port]> [-f <config-file>] [-i] [-r]")
public class ExportCommand extends RemoteCommand {
  @Parameter(names = {"-s"}, required = true, description = "Node to connect to", converter = InetSocketAddressConverter.class)
  private InetSocketAddress node;

  @Parameter(names = {"-f"}, description = "Output configuration file", converter = PathConverter.class)
  private Path outputFile;

  @Parameter(names = {"-i"}, description = "Include default values. Default: false", converter = BooleanConverter.class)
  private boolean includeDefaultValues;

  @Parameter(names = {"-r"}, description = "Export the runtime configuration instead of the configuration saved on disk. Default: false", converter = BooleanConverter.class)
  private boolean wantsRuntimeConfig;

  @Parameter(names = {"-t"}, hidden = true, description = "Output type (properties|json). Default: properties", converter = OutputFormat.FormatConverter.class)
  private OutputFormat outputFormat = OutputFormat.PROPERTIES;

  @Inject public ObjectMapperFactory objectMapperFactory;

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
      logger.info(output);

    } else {
      try {
        if (Files.exists(outputFile)) {
          logger.warn(outputFile + " already exists. Replacing this file.");
        } else {
          // try to create the parent directories
          Path dir = outputFile.toAbsolutePath().getParent();
          if (dir != null && !Files.exists(dir)) {
            Files.createDirectories(dir);
          }
        }
        Files.write(outputFile, output.getBytes(StandardCharsets.UTF_8));
        logger.info("Output saved to: {}" + lineSeparator(), outputFile);
        logger.info("Command successful!" + lineSeparator());
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
  }

  private String buildOutput(Cluster cluster, OutputFormat outputFormat) {
    switch (outputFormat) {
      case JSON:
        try {
          return objectMapperFactory.pretty().create()
              // shows optional values that are unset
              .setSerializationInclusion(JsonInclude.Include.ALWAYS)
              .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
              .writeValueAsString(cluster);
        } catch (JsonProcessingException e) {
          throw new AssertionError(outputFormat);
        }
      case PROPERTIES:
        // user-defined
        Properties userDefined = cluster.toProperties(false, false, false);
        // hidden ones
        Properties hidden = cluster.toProperties(false, false, true);
        hidden.keySet().removeAll(userDefined.keySet());
        // defaulted values
        Properties defaults = cluster.toProperties(false, true, false);
        defaults.keySet().removeAll(userDefined.keySet());
        // write them all
        try (StringWriter out = new StringWriter()) {
          // this one is always non empty since we have at least failover-priority
          Props.store(out, userDefined, "User-defined configurations");
          if (!defaults.isEmpty() && includeDefaultValues) {
            out.write(Props.EOL);
            Props.store(out, defaults, "Default configurations");
          }
          if (!hidden.isEmpty()) {
            out.write(Props.EOL);
            Props.store(out, hidden, "Hidden system configurations (only for informational, import and repair purposes): please do not alter, get, set, unset them.");
          }
          return out.toString().replace("\n", System.lineSeparator()); // to please the user. Props always writes the same way (\n).
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      default:
        throw new AssertionError(outputFormat);
    }
  }
}
