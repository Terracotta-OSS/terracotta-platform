/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.XmlConfigMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class FileConfigStorage implements ConfigStorage<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigStorage.class);

  private final Path root;
  private final String nodeName;
  private final XmlConfigMapper xmlConfigMapper;

  public FileConfigStorage(Path root, String nodeName, XmlConfigMapper xmlConfigMapper) {
    this.root = requireNonNull(root);
    this.nodeName = requireNonNull(nodeName);
    this.xmlConfigMapper = requireNonNull(xmlConfigMapper);
  }

  @Override
  public NodeContext getConfig(long version) throws ConfigStorageException {
    Path file = toPath(version);
    LOGGER.debug("Loading version: {} from file: {}", version, file);

    try {
      return xmlConfigMapper.fromXml(nodeName, new String(Files.readAllBytes(file), UTF_8));
    } catch (IOException e) {
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
      Files.write(file, xmlConfigMapper.toXml(config).getBytes(UTF_8));
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
        ClusterConfigFilename ccf = ClusterConfigFilename.from(filename);
        if (ccf.getNodeName() != null && ccf.getVersion() > 0) {
          Path backup = config.resolveSibling("backup-" + filename + "-" + time);
          try {
            Files.move(config, backup);
          } catch (IOException ioe) {
            if (error.get() == null) {
              error.set(new ConfigStorageException(ioe));
            } else {
              error.get().addSuppressed(ioe);
            }
          }
        }
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
