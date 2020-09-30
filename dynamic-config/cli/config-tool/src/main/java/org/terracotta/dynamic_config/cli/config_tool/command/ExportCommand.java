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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.cli.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.config_tool.converter.OutputFormat;
import org.terracotta.json.ObjectMapperFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

import static java.lang.System.lineSeparator;

public class ExportCommand extends RemoteCommand {
  private InetSocketAddress node;
  private Path outputFile;
  private boolean includeDefaultValues;
  private boolean wantsRuntimeConfig;
  private OutputFormat outputFormat = OutputFormat.PROPERTIES;

  @Inject public ObjectMapperFactory objectMapperFactory;

  public void setNode(InetSocketAddress node) {
    this.node = node;
  }

  public void setOutputFile(Path outputFile) {
    this.outputFile = outputFile;
  }

  public void setIncludeDefaultValues(boolean includeDefaultValues) {
    this.includeDefaultValues = includeDefaultValues;
  }

  public void setWantsRuntimeConfig(boolean wantsRuntimeConfig) {
    this.wantsRuntimeConfig = wantsRuntimeConfig;
  }

  public void setOutputFormat(OutputFormat outputFormat) {
    this.outputFormat = outputFormat;
  }
  
  @Override
  public final void run() {
    if (outputFile != null && outputFile.toFile().exists() && !Files.isRegularFile(outputFile)) {
      throw new IllegalArgumentException(outputFile + " is not a file");
    }
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    String output = buildOutput(cluster, outputFormat);

    if (outputFile == null) {
      logger.info(output);

    } else {
      try {
        if (outputFile.toFile().exists()) {
          logger.warn(outputFile + " already exists. Replacing this file.");
        } else {
          // try to create the parent directories
          Path dir = outputFile.toAbsolutePath().getParent();
          if (dir != null && !dir.toFile().exists()) {
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
          // write a timestamp as a comment in the file header
          out.write("# Timestamp of configuration export: ");
          out.write(Instant.now().toString());
          out.write(Props.EOL);
          out.write("#");
          out.write(Props.EOL);
          
          // this one is always non empty since we have at least failover-priority
          Props.store(out, userDefined, "User-defined configurations");
          if (!defaults.isEmpty() && includeDefaultValues) {
            out.write(Props.EOL);
            Props.store(out, defaults, "Default configurations");
          }
          if (!hidden.isEmpty()) {
            out.write(Props.EOL);
            Props.store(out, hidden, "Hidden internal system configurations (only for informational, import and repair purposes): please do not alter, get, set, unset them.");
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
