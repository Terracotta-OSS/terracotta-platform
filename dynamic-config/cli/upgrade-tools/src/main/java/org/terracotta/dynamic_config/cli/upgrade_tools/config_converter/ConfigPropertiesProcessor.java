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
package org.terracotta.dynamic_config.cli.upgrade_tools.config_converter;

import org.terracotta.config.data_roots.DataDirsConfig;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.service.Props;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.StandardOpenOption.CREATE;

public class ConfigPropertiesProcessor {
  private final Path outputDir;
  private final String fileName;

  public ConfigPropertiesProcessor(Path outputDir, String fileName) {
    this.outputDir = outputDir;
    this.fileName = fileName == null ? "cluster" : DataDirsConfig.cleanStringForPath(fileName);
  }

  public void process(Cluster cluster) {
    Properties properties = cluster.toProperties(false, false, true);
    try (StringWriter out = new StringWriter()) {
      Props.store(out, properties, "Converted cluster configuration:");
      Files.createDirectories(outputDir);
      Files.write(outputDir.resolve(fileName + ".properties"), out.toString().getBytes(UTF_8), CREATE);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}