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

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.RawPath;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;
import org.terracotta.dynamic_config.server.api.PathResolver;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.terracotta.config.data_roots.Utils.isEmpty;
import static org.terracotta.config.data_roots.Utils.overLaps;

public class MetaDataDirConfigChangeHandler implements ConfigChangeHandler {
  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;

  public MetaDataDirConfigChangeHandler(IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver) {
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (!change.hasValue()) {
      throw new InvalidConfigChangeException("Operation not supported");//unset not supported
    }
    RawPath metaDirPath = baseConfig.getNode().getMetadataDir().orDefault();
    RawPath changePath = RawPath.valueOf(change.getValue().get());
    Path substitutedNewMetaDirPath = parameterSubstitutor.substitute(pathResolver.resolve(changePath.toPath()));

    if (!substitutedNewMetaDirPath.toFile().exists()) {
      try {
        Files.createDirectories(substitutedNewMetaDirPath);
      } catch (Exception e) {
        throw new InvalidConfigChangeException(e.toString(), e);
      }
    } else {
      if (!Files.isDirectory(substitutedNewMetaDirPath)) {
        throw new InvalidConfigChangeException(substitutedNewMetaDirPath + " exists, but is not a directory");
      }

      if (!Files.isReadable(substitutedNewMetaDirPath)) {
        throw new InvalidConfigChangeException("Directory: " + substitutedNewMetaDirPath + " doesn't have read permissions" +
            " for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
      }

      if (!Files.isWritable(substitutedNewMetaDirPath)) {
        throw new InvalidConfigChangeException("Directory: " + substitutedNewMetaDirPath + " doesn't have write permissions" +
            " for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
      }

      if (!isEmpty(substitutedNewMetaDirPath)) {
        throw new InvalidConfigChangeException(substitutedNewMetaDirPath + " should be clean. Please clean the directory before attempting any repair");
      }
    }

    if (!metaDirPath.equals(changePath)) {
      Path substitutedExistingPath = parameterSubstitutor.substitute(pathResolver.resolve(metaDirPath.toPath()));
      if (!substitutedExistingPath.equals(substitutedNewMetaDirPath)) {
        try {
          if (overLaps(substitutedExistingPath, substitutedNewMetaDirPath)) {
            throw new InvalidConfigChangeException("Path for metadata-dir cannot be updated because " +
                "the new path overlaps with the existing path: " + metaDirPath);
          }
          // For handling cases where multiple nodes are using same parent metadata-dir path but we want to 
          // change metadata-dir for specific node.
          String nodeName = baseConfig.getNode().getName();
          new MoveOperation(substitutedNewMetaDirPath).prepare(substitutedExistingPath.resolve(nodeName));
        } catch (IOException e) {
          throw new InvalidConfigChangeException(e.toString(), e);
        }
      }
    }
  }
}
