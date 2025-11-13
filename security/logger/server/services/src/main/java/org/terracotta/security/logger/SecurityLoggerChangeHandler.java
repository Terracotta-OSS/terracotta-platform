/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
package org.terracotta.security.logger;

import java.io.IOException;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.server.InvalidConfigChangeException;
import org.terracotta.dynamic_config.api.server.PathResolver;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Mathieu Carbou
 */
class SecurityLoggerChangeHandler implements ConfigChangeHandler {

  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;

  SecurityLoggerChangeHandler(IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver) {
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;
  }

  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    if (!change.hasValue()) {
      // unset
      return;
    }

    final Path localPath;

    try {
      localPath = Files.createDirectories(parameterSubstitutor.substitute(pathResolver.resolve(Paths.get(change.getValue().get()))).normalize());
    } catch (IOException e) {
      throw new InvalidConfigChangeException("Target security-log-dir: " + change.getValue().get() + " is invalid: " + e.getMessage(), e);
    }

    if (!Files.isReadable(localPath)) {
      throw new InvalidConfigChangeException("Target security-log-dir: " + localPath + " doesn't have read permissions for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
    }

    if (!Files.isWritable(localPath)) {
      throw new InvalidConfigChangeException("Target security-log-dir: " + localPath + " doesn't have write permissions for the user: " + parameterSubstitutor.substitute("%n") + " running the server process");
    }
  }
}
