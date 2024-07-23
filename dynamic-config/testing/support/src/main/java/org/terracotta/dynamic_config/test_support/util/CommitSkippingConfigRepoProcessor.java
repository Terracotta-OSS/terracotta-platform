/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.cli.upgrade_tools.config_converter.ConfigRepoProcessor;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.NomadServerAdapter;

import java.nio.file.Path;

public class CommitSkippingConfigRepoProcessor extends ConfigRepoProcessor {

  public CommitSkippingConfigRepoProcessor(Path outputFolderPath) {
    super(outputFolderPath);
  }

  @Override
  protected NomadServer<NodeContext> getNomadServer(Cluster cluster, Node node) {
    return new NomadServerAdapter<NodeContext>(super.getNomadServer(cluster, node)) {
      @Override
      public AcceptRejectResponse commit(CommitMessage message) {
        return AcceptRejectResponse.accept();
      }
    };
  }
}