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
package org.terracotta.dynamic_config.cli.api.command;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ConfigFormat;
import org.terracotta.dynamic_config.api.service.ConfigPropertiesTranslator;
import org.terracotta.dynamic_config.api.service.Props;
import org.terracotta.dynamic_config.cli.api.command.Injector.Inject;
import org.terracotta.dynamic_config.cli.api.output.FileOutputService;
import org.terracotta.json.ObjectMapperFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Properties;

public class ExportAction extends RemoteAction {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportAction.class);

  private InetSocketAddress node;
  private Path outputFile;
  private boolean includeDefaultValues;
  private boolean wantsRuntimeConfig;
  private ConfigFormat outputFormat = ConfigFormat.CONFIG;

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

  public void setOutputFormat(ConfigFormat outputFormat) {
    this.outputFormat = outputFormat;
  }

  @Override
  public final void run() {
    if (outputFile != null && outputFile.toFile().exists() && !Files.isRegularFile(outputFile)) {
      throw new IllegalArgumentException(outputFile + " is not a file");
    }
    Cluster cluster = wantsRuntimeConfig ? getRuntimeCluster(node) : getUpcomingCluster(node);
    String out = buildOutput(cluster, outputFile);

    if (outputFile != null) {
      if (outputFile.toFile().exists()) {
        LOGGER.warn(outputFile + " already exists. Replacing this file.");
      } else {
        // try to create the parent directories
        Path dir = outputFile.toAbsolutePath().getParent();
        if (dir != null && !dir.toFile().exists()) {
          try {
            Files.createDirectories(dir);
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        }
      }
      try {
        output = new FileOutputService(outputFile);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    try {
      output.out(out);
      output.info("Command successful!");
    } finally {
      output.close();
    }
  }

  private String buildOutput(Cluster cluster, Path outputFile) {
    ConfigFormat outputFormat = outputFile == null ? this.outputFormat : ConfigFormat.from(outputFile);
    switch (outputFormat) {
      case JSON:
        try {
          return objectMapperFactory.pretty().create()
              // shows optional values that are unset
              .setSerializationInclusion(JsonInclude.Include.ALWAYS)
              .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS)
              .writeValueAsString(cluster);
        } catch (JsonProcessingException e) {
          throw new AssertionError(e);
        }
      case CONFIG:
      case PROPERTIES:
        // user-defined
        Properties userDefined = cluster.toProperties(false, false, false);
        // hidden ones
        Properties hidden = cluster.toProperties(false, false, true);
        hidden.keySet().removeAll(userDefined.keySet());
        // defaulted values
        Properties defaults = cluster.toProperties(false, true, false);
        defaults.keySet().removeAll(userDefined.keySet());

        // write a timestamp as a comment in the file header
        String fileHeader = "Timestamp of configuration export: " + Instant.now().toString();
        String userDefinedHeader = "User-defined configurations";
        String defaultHeader = "Default configurations";
        String hiddenHeader = "Hidden internal system configurations (only for informational, import and repair purposes): please do not alter, get, set, unset them.";

        if (outputFormat == ConfigFormat.PROPERTIES) {
          try (StringWriter out = new StringWriter()) {
            out.write("# " + fileHeader + Props.EOL);
            // this one is always non empty since we have at least failover-priority
            Props.store(out, userDefined, userDefinedHeader);
            if (!defaults.isEmpty() && includeDefaultValues) {
              out.write(Props.EOL);
              Props.store(out, defaults, defaultHeader);
            }
            if (!hidden.isEmpty()) {
              out.write(Props.EOL);
              Props.store(out, hidden, hiddenHeader);
            }
            return out.toString().replace("\n", System.lineSeparator()); // to please the user. Props always writes the same way (\n).
          } catch (IOException e) {
            throw new UncheckedIOException(e);
          }
        } else {
          ConfigPropertiesTranslator translator = new ConfigPropertiesTranslator();
          return translator.writeConfigOutput(fileHeader,
              userDefined, userDefinedHeader,
              includeDefaultValues ? defaults : null, defaultHeader,
              !hidden.isEmpty() ? hidden : null, hiddenHeader);
        }
      default:
        // unknown format
        throw new IllegalArgumentException("Invalid format: " + outputFormat + ". Supported formats: " + String.join(", ", ConfigFormat.supported()));
    }
  }
}
