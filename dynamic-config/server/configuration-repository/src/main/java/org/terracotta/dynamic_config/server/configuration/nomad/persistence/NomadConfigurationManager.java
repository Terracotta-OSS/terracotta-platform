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
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.FULL;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.NONE;
import static org.terracotta.dynamic_config.server.configuration.nomad.persistence.NomadConfigurationManager.ConfigDirDepth.ROOT_ONLY;

public class NomadConfigurationManager {

  static final String DIR_CLUSTER = "cluster";
  static final String DIR_LICENSE = "license";
  static final String DIR_CHANGES = "changes";

  private final Path rootPath;
  private final Path clusterPath;
  private final Path licensePath;
  private final Path changesPath;
  private final IParameterSubstitutor parameterSubstitutor;

  public NomadConfigurationManager(Path configurationDirectory, IParameterSubstitutor parameterSubstitutor) {
    this.parameterSubstitutor = parameterSubstitutor;
    requireNonNull(configurationDirectory);
    requireNonNull(parameterSubstitutor);

    // substitute path eagerly as this class needs to interact with the file system for all its functionalities
    this.rootPath = parameterSubstitutor.substitute(configurationDirectory).toAbsolutePath();
    this.clusterPath = rootPath.resolve(DIR_CLUSTER);
    this.licensePath = rootPath.resolve(DIR_LICENSE);
    this.changesPath = rootPath.resolve(DIR_CHANGES);
  }

  public Optional<String> getNodeName() {
    return findNodeName(rootPath, parameterSubstitutor);
  }

  public void createDirectories() {
    ConfigDirDepth depth = getConfigurationDirectoryDepth();
    if (depth == NONE) {
      createNomadRoot();
    }

    if (depth == NONE || depth == ROOT_ONLY) {
      createNomadSubDirectories();
    }
  }

  public Path getConfigurationDirectory() {
    return rootPath;
  }

  public Path getClusterPath() {
    return clusterPath;
  }

  public Path getLicensePath() {
    return licensePath;
  }

  public Path getChangesPath() {
    return changesPath;
  }

  ConfigDirDepth getConfigurationDirectoryDepth() {
    boolean rootExists = checkDirectoryExists(rootPath);
    boolean clusterPathExists = checkDirectoryExists(clusterPath);
    boolean licensePathExists = checkDirectoryExists(licensePath);
    boolean changesPathExists = checkDirectoryExists(changesPath);

    if (rootExists && changesPathExists && clusterPathExists && licensePathExists) {
      return FULL;
    }
    if (rootExists && !changesPathExists && !clusterPathExists && !licensePathExists) {
      return ROOT_ONLY;
    }
    if (!rootExists && !changesPathExists && !clusterPathExists && !licensePathExists) {
      return NONE;
    }
    throw new IllegalStateException("Configuration directory is partially formed. A valid configuration directory should contain '" + DIR_CLUSTER + "', '" + DIR_LICENSE + "', and '" + DIR_CHANGES + "' directories");
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
      Files.createDirectories(clusterPath);
      Files.createDirectories(licensePath);
      Files.createDirectories(changesPath);
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

  enum ConfigDirDepth {
    FULL,
    ROOT_ONLY,
    NONE
  }

  /**
   * Returns the node name from a configuration file contained in a fully-formed configuration directory.
   *
   * @param configurationDirectory configuration directory
   * @return {@code Optional} containing the node name, or an empty {@code Optional} if a node name couldn't be found
   * @throws IllegalStateException if the configuration directory is malformed
   * @throws UncheckedIOException  if an {@code IOException} occurs while reading the configuration file
   */
  public static Optional<String> findNodeName(Path configurationDirectory, IParameterSubstitutor parameterSubstitutor) {
    requireNonNull(configurationDirectory);

    NomadConfigurationManager nomadConfigurationManager = new NomadConfigurationManager(configurationDirectory, parameterSubstitutor);
    ConfigDirDepth depth = nomadConfigurationManager.getConfigurationDirectoryDepth();
    if (depth == FULL) {
      try (Stream<Path> pathStream = Files.list(nomadConfigurationManager.getClusterPath())) {
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
                  nomadConfigurationManager.getClusterPath()
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