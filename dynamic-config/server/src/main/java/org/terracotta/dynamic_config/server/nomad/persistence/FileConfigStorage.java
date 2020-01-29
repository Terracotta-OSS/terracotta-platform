/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.XmlConfigMapper;
import org.terracotta.dynamic_config.server.nomad.repository.ClusterConfigFilename;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;

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

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED_BAD_PRACTICE")
  public void saveConfig(long version, NodeContext config) throws ConfigStorageException {
    File file = toFile(version);
    file.getParentFile().mkdirs();
    LOGGER.debug("Saving topology: {} with version: {} to file: {}", config, version, file);

    try (PrintWriter writer = new PrintWriter(file, UTF_8.name())) {
      writer.print(xmlConfigMapper.toXml(config));
    } catch (FileNotFoundException | UnsupportedEncodingException e) {
      throw new ConfigStorageException(e);
    }
  }

  private File toFile(long version) {
    Path filePath = toPath(version);
    return filePath.toFile();
  }

  private Path toPath(long version) {
    String filename = ClusterConfigFilename.with(nodeName, version).toString();
    return root.resolve(filename);
  }
}
