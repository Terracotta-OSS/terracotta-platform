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
package org.terracotta.dynamic_config.test_support.util;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.conversion.ConfigRepoProcessor;
import org.terracotta.nomad.server.NomadServer;

import java.nio.file.Path;
import java.util.Map;

public class ConversionITResultProcessor extends ConfigRepoProcessor {
  private final Map<String, NomadServer<NodeContext>> serverMap;

  public ConversionITResultProcessor(Path outputFolderPath, Map<String, NomadServer<NodeContext>> serverMap) {
    super(outputFolderPath);
    this.serverMap = serverMap;
  }

  @Override
  protected NomadServer<NodeContext> getNomadServer(final int stripeId, final String nodeName) {
    NomadServer<NodeContext> nomadServer = super.getNomadServer(stripeId, nodeName);
    serverMap.put("stripe" + stripeId + "_" + nodeName, nomadServer);
    return nomadServer;
  }
}