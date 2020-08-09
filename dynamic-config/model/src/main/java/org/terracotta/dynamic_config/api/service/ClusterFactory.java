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
package org.terracotta.dynamic_config.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Version;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.lang.System.lineSeparator;
import static java.util.Map.Entry.comparingByKey;
import static java.util.stream.Collectors.toMap;

/**
 * Parses CLI or config file into a validated cluster object
 */
public class ClusterFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterFactory.class);

  private final Version version;

  public ClusterFactory() {
    this(Version.CURRENT);
  }

  public ClusterFactory(Version version) {
    this.version = version;
  }

  /**
   * Creates a {@code Cluster} object from a config properties file
   *
   * @param configFile the path to the config properties file, non-null
   * @return a {@code Cluster} object
   */
  public Cluster create(Path configFile) {
    return create(Props.load(configFile));
  }

  public Cluster create(Properties properties) {
    Collection<Configuration> defaultsAdded = new TreeSet<>(Comparator.comparing(Configuration::toString));
    Cluster cluster = create(properties, defaultsAdded::add);

    // keep that in trace because DynamicConfigConfiguration is responsible of the logging
    LOGGER.trace(
        String.format(
            "%sRead the following configurations: %s%sAdded the following defaults: %s",
            lineSeparator(),
            toDisplayParams(properties),
            lineSeparator(),
            toDisplayParams(defaultsAdded)
        )
    );

    return validated(cluster);
  }

  public Cluster create(Properties properties, Consumer<Configuration> added) {
    return ConfigurationParser.parsePropertyConfiguration(properties, version, added);
  }

  /**
   * Creates a {@code Cluster} object from a parameter-value mapping constructed from user input.
   *
   * @param paramValueMap parameter-value mapping
   * @return a {@code Cluster} object
   */
  public Cluster create(Map<Setting, String> paramValueMap, IParameterSubstitutor parameterSubstitutor) {
    // safe copy
    paramValueMap = new HashMap<>(paramValueMap);

    Collection<Configuration> defaultsAdded = new TreeSet<>(Comparator.comparing(Configuration::toString));
    Cluster cluster = ConfigurationParser.parseCommandLineParameters(paramValueMap, parameterSubstitutor, defaultsAdded::add);

    // keep that in trace because DynamicConfigConfiguration is responsible of the logging
    LOGGER.trace(
        String.format(
            "%sRead the following parameters: %s%sAdded the following defaults: %s",
            lineSeparator(),
            toDisplayParams("--", paramValueMap, parameterSubstitutor),
            lineSeparator(),
            toDisplayParams("--", defaultsAdded.stream()
                    .filter(configuration -> configuration.getValue().isPresent())
                    .collect(toMap(Configuration::getSetting, cfg -> cfg.getValue().get())),
                parameterSubstitutor)
        )
    );

    return validated(cluster);
  }

  private Cluster validated(Cluster cluster) {
    new ClusterValidator(cluster).validate(version);
    return cluster;
  }

  private String toDisplayParams(String prefix, Map<Setting, String> supplied, IParameterSubstitutor parameterSubstitutor) {
    String suppliedParameters = supplied.entrySet()
        .stream()
        .filter(e -> e.getValue() != null)
        .sorted(comparingByKey())
        .map(entry -> prefix + entry.getKey() + "=" + parameterSubstitutor.substitute(entry.getValue()))
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }

  private String toDisplayParams(Properties properties) {
    String suppliedParameters = properties.entrySet()
        .stream()
        .filter(e -> e.getValue() != null && !e.getValue().equals(""))
        .sorted(Comparator.comparing(e -> e.getKey().toString()))
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }

  private String toDisplayParams(Collection<Configuration> configurations) {
    String suppliedParameters = configurations.stream()
        .filter(c -> c.getValue().isPresent())
        .map(Configuration::toString)
        .sorted()
        .collect(Collectors.joining(lineSeparator() + "    ", "    ", ""));
    if (suppliedParameters.trim().isEmpty()) {
      suppliedParameters = "[]";
    } else {
      suppliedParameters = lineSeparator() + suppliedParameters;
    }
    return suppliedParameters;
  }
}
