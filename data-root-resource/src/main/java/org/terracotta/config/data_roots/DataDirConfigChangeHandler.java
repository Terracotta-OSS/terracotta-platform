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
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.dynamic_config.server.api.MoveOperation;
import org.terracotta.dynamic_config.server.api.PathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * Handles dynamic data-directory additions
 */
public class DataDirConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(DataDirConfigChangeHandler.class);

  private final DataDirsConfig dataDirsConfig;
  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;

  public DataDirConfigChangeHandler(DataDirsConfig dataDirsConfig, IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver) {
    this.dataDirsConfig = dataDirsConfig;
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration changes) throws InvalidConfigChangeException {
    if (!changes.hasValue()) {
      throw new InvalidConfigChangeException("Operation not supported");//unset not supported
    }

    for (Configuration change : changes.expand()) {
      Map<String, RawPath> dataDirs = baseConfig.getNode().getDataDirs().orDefault().entrySet().stream().collect(toMap(Map.Entry::getKey, e -> e.getValue()));
      LOGGER.debug("Validating change: {} against node data directories: {}", change, dataDirs);

      String dataDirectoryName = change.getKey();
      RawPath changePath = RawPath.valueOf(change.getValue().get());
      Path substitutedDataDirPath = substitute(changePath.toPath());

      if (!dataDirs.containsKey(dataDirectoryName)) {
        for (Map.Entry<String, RawPath> entry : dataDirs.entrySet()) {
          if (overLaps(substitute(entry.getValue().toPath()), substitutedDataDirPath)) {
            throw new InvalidConfigChangeException("Data directory: " + dataDirectoryName +
                " overlaps with existing data directory: " + entry.getKey() + " " + entry.getValue());
          }
        }
      }

      if (dataDirs.containsKey(dataDirectoryName)) {
        if (!dataDirs.get(dataDirectoryName).equals(changePath)) {
          Path substitutedExistingPath = substitute(dataDirs.get(dataDirectoryName).toPath());
          if (!substitutedExistingPath.equals(substitutedDataDirPath)) {
            if (overLaps(substitutedExistingPath, substitutedDataDirPath)) {
              throw new InvalidConfigChangeException("Path for data-dir: " + dataDirectoryName +
                  " cannot be updated because the new path overlaps with the existing path: " + dataDirs.get(dataDirectoryName));
            }
          }
        }
      }

      if (!substitutedDataDirPath.toFile().exists()) {
        try {
          Files.createDirectories(substitutedDataDirPath);
        } catch (Exception e) {
          throw new InvalidConfigChangeException(e.toString(), e);
        }
      } else {
        if (!Files.isDirectory(substitutedDataDirPath)) {
          throw new InvalidConfigChangeException(substitutedDataDirPath + " exists, but is not a directory");
        }

        if (!Files.isReadable(substitutedDataDirPath)) {
          throw new InvalidConfigChangeException("Directory: " + substitutedDataDirPath + " doesn't have read permissions" +
              " for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
        }

        if (!Files.isWritable(substitutedDataDirPath)) {
          throw new InvalidConfigChangeException("Directory: " + substitutedDataDirPath + " doesn't have write permissions" +
              " for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
        }
      }

      // updating the path for data-dir name.
      if (dataDirs.containsKey(dataDirectoryName)) {
        if (!dataDirs.get(dataDirectoryName).equals(changePath)) {
          Path substitutedExistingPath = substitute(dataDirs.get(dataDirectoryName).toPath());
          if (!substitutedExistingPath.equals(substitutedDataDirPath)) {
            try {
              // For handling cases where multiple nodes are using same parent data-dir path but we want to 
              // change data-dir for specific node.
              String nodeName = baseConfig.getNode().getName();
              new MoveOperation(substitutedDataDirPath).prepare(substitutedExistingPath.resolve(nodeName));
            } catch (IOException e) {
              throw new InvalidConfigChangeException(e.toString(), e);
            }
          }
        }
      }
    }
  }

  @Override
  public void apply(Configuration changes) {
    for (Configuration change : changes.expand()) {
      String dataDirectoryName = change.getKey();
      String dataDirectoryPath = change.getValue().get();
      dataDirsConfig.addDataDirectory(dataDirectoryName, dataDirectoryPath);
    }
  }

  public boolean overLaps(Path existing, Path newDataDirPath) {
    return existing.startsWith(newDataDirPath) || newDataDirPath.startsWith(existing);
  }

  private Path substitute(Path path) {
    return parameterSubstitutor.substitute(pathResolver.resolve(path)).normalize();
  }
}
