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
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

/**
 * Handles dynamic data-directory additions
 */
public class DataDirectoryConfigChangeHandler implements ConfigChangeHandler {
  private final DataDirectoriesConfig dataDirectoriesConfig;
  private final IParameterSubstitutor parameterSubstitutor;

  public DataDirectoryConfigChangeHandler(DataDirectoriesConfig dataDirectoriesConfig, IParameterSubstitutor parameterSubstitutor) {
    this.dataDirectoriesConfig = dataDirectoriesConfig;
    this.parameterSubstitutor = parameterSubstitutor;
  }

  @Override
  public void validate(NodeContext baseConfig, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    try {
      String dataDirectoryName = change.getKey();
      String dataDirectoryPath = change.getValue();
      dataDirectoriesConfig.validateDataDirectory(dataDirectoryName, dataDirectoryPath);
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public void apply(Configuration change) {
    String dataDirectoryName = change.getKey();
    String dataDirectoryPath = parameterSubstitutor.substitute(change.getValue());
    dataDirectoriesConfig.addDataDirectory(dataDirectoryName, dataDirectoryPath);
  }
}
