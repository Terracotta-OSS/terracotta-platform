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

import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.FULL;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.NONE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadRepositoryManager.RepositoryDepth.ROOT_ONLY;

public class NomadRepositoryManager {
  private static final String CONFIG = "config";
  private static final String LICENSE = "license";
  private static final String SANSKRIT = "sanskrit";

  private final Path rootPath;
  private final Path configPath;
  private final Path licensePath;
  private final Path sanskritPath;
  private final IParameterSubstitutor parameterSubstitutor;

  public NomadRepositoryManager(Path configRepositoryDir, IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
    requireNonNull(configRepositoryDir);
    requireNonNull(parameterSubstitutor);

    // substitute path eagerly as this class needs to interact with the file system for all its functionalities
    this.rootPath = parameterSubstitutor.substitute(configRepositoryDir).toAbsolutePath();
    this.configPath = rootPath.resolve(CONFIG);
    this.licensePath = rootPath.resolve(LICENSE);
    this.sanskritPath = rootPath.resolve(SANSKRIT);
  }

  public Optional<String> getNodeName() {
    return findNodeName(rootPath, parameterSubstitutor);
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

  public Path getConfigRepositoryDir() {
    return rootPath;
  }

  public Path getConfigPath() {
    return configPath;
  }

  public Path getLicensePath() {
    return licensePath;
  }

  public Path getSanskritPath() {
    return sanskritPath;
  }

  RepositoryDepth getRepositoryDepth() {
    boolean nomadRootExists = checkDirectoryExists(rootPath);
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
    throw new IllegalStateException("Repository is partially formed. A valid repository should contain '" + CONFIG + "', '" + LICENSE + "', and '" + SANSKRIT + "' directories");
  }

  boolean checkDirectoryExists(Path path) {
    boolean dirExists = Files.exists(path);
    if (dirExists && !Files.isDirectory(path)) {
      throw new IllegalArgumentException(path.getFileName() + " at path: " + path.getParent() + " is not a directory");
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
      Files.createDirectories(rootPath);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  enum RepositoryDepth {
    FULL,
    ROOT_ONLY,
    NONE
  }

  /**
   * Returns the node name from a configuration file contained in a fully-formed config repository.
   *
   * @param nomadRoot repository root
   * @return {@code Optional} containing the node name, or an empty {@code Optional} if a node name couldn't be found
   * @throws IllegalStateException if the repository is malformed
   * @throws UncheckedIOException  if an {@code IOException} occurs while reading the configuration file
   */
  public static Optional<String> findNodeName(Path nomadRoot, IParameterSubstitutor parameterSubstitutor) {
    requireNonNull(nomadRoot);

    NomadRepositoryManager nomadRepositoryManager = new NomadRepositoryManager(nomadRoot, parameterSubstitutor);
    RepositoryDepth repositoryDepth = nomadRepositoryManager.getRepositoryDepth();
    if (repositoryDepth == FULL) {
      try (Stream<Path> pathStream = Files.list(nomadRepositoryManager.getConfigPath())) {
        Set<String> nodeNames = pathStream
            .map(Path::getFileName)
            .map(Path::toString)
            .map(ClusterConfigFilename::from)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(ClusterConfigFilename::getNodeName)
            .collect(toSet());
        if (nodeNames.size() > 1) {
          throw new IllegalStateException(
              String.format("Found versioned cluster config files for the following different nodes: %s in: %s",
                  String.join(", ", nodeNames),
                  nomadRepositoryManager.getConfigPath()
              )
          );
        } else if (nodeNames.size() == 1) {
          return Optional.of(nodeNames.iterator().next());
        }
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }
    return Optional.empty();
  }
}