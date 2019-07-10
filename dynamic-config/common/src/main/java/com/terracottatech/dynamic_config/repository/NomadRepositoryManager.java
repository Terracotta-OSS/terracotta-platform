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

import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.FULL;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.NONE;
import static com.terracottatech.dynamic_config.repository.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;
import static java.util.Objects.requireNonNull;

public class NomadRepositoryManager {
  private static final String CONFIG = "config";
  private static final String LICENSE = "license";
  private static final String SANSKRIT = "sanskrit";

  private final Path nomadRoot;
  private final Path configPath;
  private final Path licensePath;
  private final Path sanskritPath;

  public NomadRepositoryManager(Path nomadRoot) {
    this.nomadRoot = requireNonNull(nomadRoot).toAbsolutePath().normalize();
    this.configPath = nomadRoot.resolve(CONFIG);
    this.licensePath = nomadRoot.resolve(LICENSE);
    this.sanskritPath = nomadRoot.resolve(SANSKRIT);
  }

  public Optional<String> getNodeName() {
    RepositoryDepth repositoryDepth = getRepositoryDepth();
    if (repositoryDepth == NONE || repositoryDepth == ROOT_ONLY) {
      return Optional.empty();
    }
    return extractNodeName();
  }

  public void createDirectories() {
    RepositoryDepth repositoryDepth = getRepositoryDepth();
    if (repositoryDepth == NONE) {
      createNomadRoot();
    }

    if (repositoryDepth == NONE || repositoryDepth == ROOT_ONLY) {
      createNomadSubDirectories();
    }
  }

  public Path getNomadRoot() {
    return nomadRoot;
  }

  public Path getConfigurationPath() {
    return configPath;
  }

  public Path getLicensePath() {
    return licensePath;
  }

  public Path getSanskritPath() {
    return sanskritPath;
  }

  Optional<String> extractNodeName() {
    return NodeNameExtractor.extractFromConfigOptional(nomadRoot);
  }

  RepositoryDepth getRepositoryDepth() {
    boolean nomadRootExists = checkDirectoryExists(nomadRoot);
    boolean configPathExists = checkDirectoryExists(configPath);
    boolean licensePathExists = checkDirectoryExists(licensePath);
    boolean sanskritPathExists = checkDirectoryExists(sanskritPath);

    if (nomadRootExists && sanskritPathExists && configPathExists && licensePathExists) {
      return FULL;
    }
    if (nomadRootExists && !sanskritPathExists && !configPathExists && !licensePathExists) {
      return ROOT_ONLY;
    }
    if (!nomadRootExists && !sanskritPathExists && !configPathExists && !licensePathExists) {
      return NONE;
    }
    throw new MalformedRepositoryException("Repository is partially formed. A valid repository should contain '" + CONFIG + "', '" + LICENSE + "', and '" + SANSKRIT + "' directories");
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
      Files.createDirectories(configPath);
      Files.createDirectories(licensePath);
      Files.createDirectories(sanskritPath);
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