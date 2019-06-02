/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.FULL;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.NONE;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.CONFIG_REPO_FILENAME_REGEX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_PREFIX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_SUFFIX;

public class NomadRepositoryManager {
  private static final String SANSKRIT = "sanskrit";
  private static final String CONFIG = "config";

  private final Path nomadRoot;
  private final Path sanskritPath;
  private final Path configPath;

  public NomadRepositoryManager(Path nomadRoot) {
    this.nomadRoot = nomadRoot;
    this.sanskritPath = nomadRoot.resolve(SANSKRIT);
    this.configPath = nomadRoot.resolve(CONFIG);
  }

  public Optional<String> getNodeName() {
    RepositoryDepth repositoryDepth = getRepositoryDepth();
    if (repositoryDepth == NONE || repositoryDepth == ROOT_ONLY) {
      return Optional.empty();
    }
    return Optional.of(extractNodeName());
  }

  public void createIfAbsent() {
    RepositoryDepth repositoryDepth = getRepositoryDepth();
    if (repositoryDepth == NONE) {
      createNomadRoot();
    }

    if (repositoryDepth == NONE || repositoryDepth == ROOT_ONLY) {
      createNomadSubDirectories();
    }
  }

  public Path getConfigurationPath() {
    return configPath;
  }

  public Path getSanskritPath() {
    return sanskritPath;
  }

  String extractNodeName() {
    try (Stream<Path> stream = Files.list(configPath)) {
      Set<String> distinctFileNames = stream.map(path -> path.getFileName().toString())
          .filter(fileName -> fileName.matches(CONFIG_REPO_FILENAME_REGEX))
          .collect(Collectors.toSet());

      if (distinctFileNames.isEmpty()) {
        throw new MalformedRepositoryException("No configuration file found in: " + configPath + ". " +
            "A valid configuration file follows the '" + CONFIG_REPO_FILENAME_REGEX + "' regular expression");
      } else {
        Set<String> nodeNames = distinctFileNames.stream().map(this::extractInternal).collect(Collectors.toSet());
        if (nodeNames.size() > 1) {
          throw new MalformedRepositoryException("Found multiple configuration files corresponding to the following different node names: " + nodeNames);
        }
        return nodeNames.iterator().next();
      }
    } catch (IOException e) {
      throw new MalformedRepositoryException("Repository is partially formed. A valid repository should contain '" + SANSKRIT + "' and '" + CONFIG + "' directories");
    }
  }

  private String extractInternal(String fileName) {
    return fileName.replaceAll("^" + REGEX_PREFIX, "").replaceAll(REGEX_SUFFIX + "$", "");
  }

  RepositoryDepth getRepositoryDepth() {
    boolean nomadRootExists = checkDirectoryExists(nomadRoot);
    boolean sanskritPathExists = checkDirectoryExists(sanskritPath);
    boolean configPathExists = checkDirectoryExists(configPath);

    if (nomadRootExists && sanskritPathExists && configPathExists) {
      return FULL;
    }
    if (nomadRootExists && !sanskritPathExists && !configPathExists) {
      return ROOT_ONLY;
    }
    if (!nomadRootExists && !sanskritPathExists && !configPathExists) {
      return NONE;
    }
    throw new MalformedRepositoryException("Repository is partially formed. A valid repository should contain '" + SANSKRIT + "' and '" + CONFIG + "' directories");
  }

  boolean checkDirectoryExists(Path path) {
    boolean dirExists = Files.exists(path);
    if (dirExists && !Files.isDirectory(path)) {
      throw new MalformedRepositoryException(path.getFileName() + " is not a directory");
    }
    return dirExists;
  }

  void createNomadSubDirectories() {
    try {
      Files.createDirectories(sanskritPath);
      Files.createDirectories(configPath);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  void createNomadRoot() {
    try {
      Files.createDirectories(nomadRoot);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  enum RepositoryDepth {
    FULL,
    ROOT_ONLY,
    NONE
  }
}