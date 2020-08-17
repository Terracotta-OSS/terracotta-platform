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
package org.terracotta.config.data_roots;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.dynamic_config.server.api.PathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Handles dynamic data-directory additions
 */
public class DataDirectoryConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataDirectoryConfigChangeHandler.class);

  private final DataDirectoriesConfig dataDirectoriesConfig;
  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;

  public DataDirectoryConfigChangeHandler(DataDirectoriesConfig dataDirectoriesConfig, IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver) {
    this.dataDirectoriesConfig = dataDirectoriesConfig;
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (!change.getValue().isPresent()) {
      throw new InvalidConfigChangeException("Operation not supported");//unset not supported
    }

    Map<String, Path> dataDirs = baseConfig.getNode().getDataDirs().orDefault();
    LOGGER.debug("Validating change: {} against node data directories: {}", change, dataDirs);

    String dataDirectoryName = change.getKey();
    Path dataDirectoryPath = Paths.get(change.getValue().get());

    if (dataDirs.containsKey(dataDirectoryName)) {
      throw new InvalidConfigChangeException("A data directory with name: " + dataDirectoryName + " already exists");
    }

    for (Map.Entry<String, Path> entry : dataDirs.entrySet()) {
      if (overLaps(entry.getValue(), dataDirectoryPath)) {
        throw new InvalidConfigChangeException("Data directory: " + dataDirectoryName + " overlaps with: " + entry.getKey());
      }
    }

    try {
      ensureDirectory(dataDirectoryPath);
    } catch (IOException e) {
      throw new InvalidConfigChangeException(e.toString(), e);
    }
  }

  @Override
  public void apply(Configuration change) {
    String dataDirectoryName = change.getKey();
    String dataDirectoryPath = change.getValue().get();
    dataDirectoriesConfig.addDataDirectory(dataDirectoryName, dataDirectoryPath);
  }

  public boolean overLaps(Path existing, Path newDataDirectoryPath) {
    Path e = compute(existing);
    Path n = compute(newDataDirectoryPath);
    return e.startsWith(n) || n.startsWith(e);
  }

  private Path compute(Path path) {
    return parameterSubstitutor.substitute(pathResolver.resolve(path)).normalize();
  }

  private void ensureDirectory(Path directory) throws IOException, InvalidConfigChangeException {
    directory = compute(directory);
    if (!directory.toFile().exists()) {
      Files.createDirectories(directory);
    } else {
      if (!Files.isDirectory(directory)) {
        throw new InvalidConfigChangeException(directory.getFileName() + " exists under " + directory.getParent() + " but is not a directory");
      }
    }
  }
}
