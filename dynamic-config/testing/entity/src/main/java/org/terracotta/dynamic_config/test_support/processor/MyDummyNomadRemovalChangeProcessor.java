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
package org.terracotta.dynamic_config.test_support.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.ClusterState;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.NodeRemovalNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;
import org.terracotta.dynamic_config.api.service.TopologyService;
import org.terracotta.dynamic_config.server.api.DynamicConfigEventFiring;
import org.terracotta.dynamic_config.server.api.NomadChangeProcessor;
import org.terracotta.dynamic_config.server.api.PathResolver;
import org.terracotta.nomad.server.NomadException;

import javax.management.MBeanServer;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.test_support.processor.ServerCrasher.crash;

public class MyDummyNomadRemovalChangeProcessor implements NomadChangeProcessor<NodeRemovalNomadChange> {
  private static final Logger LOGGER = LoggerFactory.getLogger(MyDummyNomadAdditionChangeProcessor.class);
  private static final String PLATFORM_MBEAN_OPERATION_NAME = "removePassive";
  private static final String failAtPrepare = "prepareDeletion-failure";
  private static final String killAtPrepare = "killDeletion-prepare";
  private static final String killAtCommit = "killDeletion-commit";
  private static final String failoverKey = "failoverDeletion";
  private static final String detachStatusKey = "detachStatus";
  private final TopologyService topologyService;
  private final DynamicConfigEventFiring dynamicConfigEventFiring;
  private final IParameterSubstitutor parameterSubstitutor;
  private final PathResolver pathResolver;
  private final MBeanServer mbeanServer;

  public MyDummyNomadRemovalChangeProcessor(TopologyService topologyService, DynamicConfigEventFiring dynamicConfigEventFiring, IParameterSubstitutor parameterSubstitutor, PathResolver pathResolver, MBeanServer mbeanServer) {
    this.mbeanServer = mbeanServer;
    this.topologyService = requireNonNull(topologyService);
    this.dynamicConfigEventFiring = requireNonNull(dynamicConfigEventFiring);
    this.parameterSubstitutor = parameterSubstitutor;
    this.pathResolver = pathResolver;
  }

  @Override
  public void validate(NodeContext baseConfig, NodeRemovalNomadChange change) throws NomadException {
    if (failAtPrepare.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(detachStatusKey))) {
      throw new NomadException("Invalid addition fail at prepare");
    }
    LOGGER.info("Validating change: {}", change.getSummary());
    if (baseConfig == null) {
      throw new NomadException("Existing config must not be null");
    }
    try {
      Cluster updated = change.apply(baseConfig.getCluster());
      new ClusterValidator(updated).validate(ClusterState.ACTIVATED);
    } catch (RuntimeException e) {
      throw new NomadException("Error when trying to apply: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
    // cause failure when in prepare phase
    if (killAtPrepare.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(failoverKey))) {
      crash();
    }
  }

  @Override
  public void apply(NodeRemovalNomadChange change) throws NomadException {
    Cluster runtime = topologyService.getRuntimeNodeContext().getCluster();
    Node node = change.getNode();
    if (!runtime.containsNode(node.getUID())) {
      return;
    }

    // cause failover when in commit phase
    if (killAtCommit.equals(topologyService.getUpcomingNodeContext().getNode().getTcProperties().orDefault().get(failoverKey))) {
      Path path = null;
      try {
        // We create a marker on disk to know that we have triggered the failover once.
        // When the node will be restarted, and the repair command triggered again to re-execute the commit,
        // the file will be there, so 'createFile()' will fail and the node won't be killed.
        // This hack is so only trigger the commit failure once
        path = path().resolve("killed");
        Files.createFile(path);
        crash();
      } catch (FileAlreadyExistsException e) {
        // this exception si normal for the second run
        LOGGER.warn(e.getMessage(), e);
        try {
          org.terracotta.utilities.io.Files.deleteIfExists(path);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    try {
      LOGGER.info("Removing node: {} from stripe UID: {}", node.getName(), change.getStripeUID());
      dynamicConfigEventFiring.onNodeRemoval(change.getStripeUID(), node);
    } catch (RuntimeException e) {
      throw new NomadException("Error when applying: '" + change.getSummary() + "': " + e.getMessage(), e);
    }
  }

  private Path path() throws IOException {
    final Path directory = parameterSubstitutor.substitute(pathResolver.resolve(topologyService.getUpcomingNodeContext().getNode().getDataDirs().orDefault().get("main").toPath())).normalize();
    if (!directory.toFile().exists()) {
      Files.createDirectories(directory);
    } else {
      if (!Files.isDirectory(directory)) {
        throw new IOException(directory.getFileName() + " exists under " + directory.getParent() + " but is not a directory");
      }
    }
    return directory;
  }
}
