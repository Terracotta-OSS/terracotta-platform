/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.exception.NomadConfigFileNameProviderException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NomadConfigFileNameProvider {

  public static Function<Long, String> getFileNameProvider(final Path configPath, final ConfigController configController) {
    return (version) -> {
      String nodeName;
      if (configController == null) {
        try (Stream<Path> walk = Files.walk(configPath)) {
          List<String> configFileNames = walk
              .filter(Files::isRegularFile)
              .map(filePath -> filePath.toString())
              .filter(pathString -> pathString.endsWith(".xml"))
              .map(pathString -> Paths.get(pathString).getFileName().toString())
              .collect(Collectors.toList());
          nodeName = getNodeName(configFileNames);
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      } else {
        nodeName = configController.getNodeName();
        if (nodeName == null) {
          throw new NomadConfigFileNameProviderException("node name null in ConfigController");
        }
      }
      return "cluster-config." + nodeName + "." + version + ".xml";
    };
  }

  public static final String getNodeName(List<String> configFileNames) {
    Set<String> nodeNames = new HashSet<>();
    if (configFileNames == null || configFileNames.size() == 0) {
      throw new NomadConfigFileNameProviderException("Missing configuration file");
    }
    for (String fileName : configFileNames) {
      String[] fileNamePart = fileName.split("\\.");
      if (fileNamePart.length != 4) {
        throw new NomadConfigFileNameProviderException("Invalid configuration file name format. " +
                                                       "Configuration file name format is " +
                                                       "cluster-config.<node-name>.<version>.xml");
      }
      if (!"cluster-config".equals(fileNamePart[0])) {
        throw new NomadConfigFileNameProviderException("Configuration file name MUST start with cluster-config");
      }
      nodeNames.add(fileNamePart[1]);
    }
    if (nodeNames.size() > 1) {
      throw new NomadConfigFileNameProviderException("Configuration file name format can not have more than one" +
                                                     " nodeName." + " Format is cluster-config.<node-name>.<version>.xml");
    }
    return nodeNames.iterator().next();
  }
}
