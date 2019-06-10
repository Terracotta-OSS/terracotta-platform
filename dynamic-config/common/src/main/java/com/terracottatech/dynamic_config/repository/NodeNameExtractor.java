/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.repository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.repository.RepositoryConstants.CONFIG_REPO_FILENAME_REGEX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_PREFIX;
import static com.terracottatech.dynamic_config.repository.RepositoryConstants.REGEX_SUFFIX;

public class NodeNameExtractor {
  public static String extractFromConfig(Path configPath) {
    try (Stream<Path> stream = Files.list(configPath)) {
      Set<String> distinctFileNames = stream.map(path -> path.getFileName().toString())
          .filter(fileName -> fileName.matches(CONFIG_REPO_FILENAME_REGEX))
          .collect(Collectors.toSet());

      if (distinctFileNames.isEmpty()) {
        throw new MalformedRepositoryException("No configuration files found in: " + configPath + ". " +
                                               "A valid configuration file follows the '" + CONFIG_REPO_FILENAME_REGEX + "' regular expression");
      } else {
        Set<String> nodeNames = distinctFileNames.stream().map(NodeNameExtractor::extractInternal).collect(Collectors.toSet());
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
