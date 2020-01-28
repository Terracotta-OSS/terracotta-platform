/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad.persistence;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.repository.ClusterConfigFilename;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PathResolver;
import com.terracottatech.dynamic_config.xml.XmlConfigMapper;
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
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class FileConfigStorage implements ConfigStorage<NodeContext> {
  private static final Logger LOGGER = LoggerFactory.getLogger(FileConfigStorage.class);

  private final Path root;
  private final String nodeName;
  private final XmlConfigMapper xmlConfigMapper;

  public FileConfigStorage(Path root, String nodeName, IParameterSubstitutor parameterSubstitutor) {
    this.root = requireNonNull(root);
    this.nodeName = requireNonNull(nodeName);
    requireNonNull(parameterSubstitutor);

    // This path resolver is used when converting a model to XML.
    // It makes sure to resolve any relative path to absolute ones based on the working directory.
    // This is necessary because if some relative path ends up in the XML exactly like they are in the model,
    // then platform will rebase these paths relatively to the config XML file which is inside a sub-folder in
    // the config repository: repository/config.
    // So this has the effect of putting all defined directories inside such as repository/config/logs, repository/config/user-data, repository/metadata, etc
    // That is why we need to force the resolving within the XML relatively to the user directory.
    PathResolver userDirResolver = new PathResolver(Paths.get("%(user.dir)"), parameterSubstitutor::substitute);
    this.xmlConfigMapper = new XmlConfigMapper(userDirResolver);
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
