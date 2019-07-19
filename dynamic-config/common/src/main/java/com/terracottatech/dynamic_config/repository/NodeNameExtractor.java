/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.repository.RepositoryConstants.CONFIG_REPO_FILENAME_REGEX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.FILENAME_EXT;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.FILENAME_PREFIX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_PREFIX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_SUFFIX;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;

public class NodeNameExtractor {
  public static Optional<String> extractFromConfigOptional(Path nomadRoot) {
    Optional<String> nodeNameOptional = Optional.empty();
    try {
      String nodeName = extractFromConfig(nomadRoot);
      return Optional.of(nodeName);
    } catch (MalformedRepositoryException e) {
      return nodeNameOptional;
    }
  }

  public static String extractFromConfig(Path nomadRoot) {
    Path configPath = requireNonNull(nomadRoot).resolve("config");
    try (Stream<Path> stream = Files.list(configPath)) {
      Collection<String> distinctFileNames = stream.map(path -> path.getFileName().toString())
          .filter(fileName -> fileName.matches(CONFIG_REPO_FILENAME_REGEX))
          .collect(toCollection(TreeSet::new));

      if (distinctFileNames.isEmpty()) {
        String format = FILENAME_PREFIX + ".<node-name>.<version>." + FILENAME_EXT;
        throw new MalformedRepositoryException(
            String.format(
                "No configuration files found in: %s. A valid configuration file is of the format: %s",
                configPath,
                format
            )
        );
      } else {
        Collection<String> nodeNames = distinctFileNames.stream().map(NodeNameExtractor::extractInternal).collect(Collectors.toCollection(TreeSet::new));
        if (nodeNames.size() > 1) {
          throw new MalformedRepositoryException("Found multiple configuration files corresponding to the following different node names: " + nodeNames);
        }
        return nodeNames.iterator().next();
      }
    } catch (IOException e) {
      throw new MalformedRepositoryException("Couldn't read information from configuration storage", e);
    }
  }

  private static String extractInternal(String fileName) {
    return fileName.replaceAll("^" + REGEX_PREFIX, "").replaceAll(REGEX_SUFFIX + "$", "");
  }
}
