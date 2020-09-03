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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.model.Version;
import org.terracotta.dynamic_config.api.model.nomad.FormatUpgradeNomadChange;
import org.terracotta.dynamic_config.api.service.ClusterFactory;
import org.terracotta.dynamic_config.api.service.ClusterValidator;
import org.terracotta.dynamic_config.api.service.Props;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Version.CURRENT;
import static org.terracotta.dynamic_config.api.model.Version.V1;

public class FileConfigStorage implements ConfigStorage {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigStorage.class);

  private final Path root;
  private final String nodeName;
  private final ClusterValidator clusterValidator;

  public FileConfigStorage(Path root, String nodeName, ClusterValidator clusterValidator) {
    this.root = requireNonNull(root);
    this.nodeName = requireNonNull(nodeName);
    this.clusterValidator = requireNonNull(clusterValidator);
  }

  @SuppressWarnings("unused")
  @Override
  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  public Config getConfig(long version) throws ConfigStorageException {
    Path file = toPath(version);
    LOGGER.debug("Loading version: {} from file: {}", version, file);
    try {
      Properties properties = Props.load(file);

      // removing extra information put in V1
      properties.remove("this.node-id");
      properties.remove("this.stripe-id");
      String nodeName = Optional.ofNullable(properties.remove("this.name"))
          .map(Object::toString)
          .orElse(null);

      // removing extra information put in V2
      UID nodeUID = Optional.ofNullable(properties.remove("this.node-uid"))
          .map(Object::toString)
          .map(UID::valueOf)
          .orElse(null);
      Version configFormatVersion = Optional.ofNullable(properties.remove("this.version"))
          .map(Object::toString)
          .map(Version::fromValue)
          .orElse(V1);

      // parse the config in the given version.
      // Note: this is really important to use the parser matching the version of the config.
      // Reason is that Nomad is computing a hash based on the "output" of the change, and verifies this hash
      // back when re-loading. So the reloaded value cannot be parsed differently.
      Cluster cluster = new ClusterFactory(clusterValidator, configFormatVersion).create(properties, configuration -> {
      }); // do not over-log added configs

      // we are eagerly applying the upgrade in memory.
      // It will be re-applied after through a nomad change and persisted
      // this si required because everything is working based on the UIDs now...
      cluster = new FormatUpgradeNomadChange(configFormatVersion, CURRENT).apply(cluster);
      clusterValidator.validate(cluster);

      // V1 => V2: nodeUID is in V2, nodeName in V1
      if (nodeUID == null) {
        nodeUID = cluster.getNodeByName(nodeName)
            .map(Node::getUID)
            .orElseThrow(() -> new IllegalStateException("Wrong config! Node: " + nodeName + " not found or no UID on this node"));
      }

      return new Config(new NodeContext(cluster, nodeUID), configFormatVersion);
    } catch (RuntimeException e) {
      throw new ConfigStorageException(e);
    }
  }

  @Override
  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
    Path file = toPath(version);
    LOGGER.debug("Saving topology: {} with version: {} to file: {}", config, version, file);
    try {
      if (file.getParent() != null) {
        Files.createDirectories(file.getParent());
      }
      Properties nonDefaults = config.getCluster().toProperties(false, false, true);

      // adds extra information about this node
      nonDefaults.setProperty("this.node-uid", String.valueOf(config.getNodeUID()));
      nonDefaults.setProperty("this.version", CURRENT.getValue());

      StringWriter out = new StringWriter();
      String comments = "THIS FILE IS INTENDED FOR BOOK-KEEPING PURPOSES ONLY, AND IS NOT SUPPOSED TO BE EDITED. DO NOT ATTEMPT TO MODIFY.";
      Props.store(out, nonDefaults, comments);
      Files.write(file, out.toString().getBytes(UTF_8));
    } catch (IOException e) {
      throw new ConfigStorageException(e);
    }
  }

  @Override
  public void reset() throws ConfigStorageException {
    String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss"));
    AtomicReference<ConfigStorageException> error = new AtomicReference<>();
    try (Stream<Path> stream = Files.list(root)) {
      stream.filter(Files::isRegularFile).forEach(config -> {
        String filename = config.getFileName().toString();
        ClusterConfigFilename.from(filename).ifPresent(ccf -> {
          Path backup = config.resolveSibling("backup-" + filename + "-" + time);
          try {
            org.terracotta.utilities.io.Files.relocate(config, backup);
          } catch (IOException ioe) {
            if (error.get() == null) {
              error.set(new ConfigStorageException(ioe));
            } else {
              error.get().addSuppressed(ioe);
            }
          }
        });
      });
    } catch (IOException e) {
      throw new ConfigStorageException(e);
    }
    if (error.get() != null) {
      throw error.get();
    }
  }

  private Path toPath(long version) {
    String filename = ClusterConfigFilename.with(nodeName, version).getFilename();
    return root.resolve(filename);
  }
}
