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

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Substitutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static org.terracotta.dynamic_config.api.model.ClusterState.CONFIGURING;
import static org.terracotta.dynamic_config.api.model.Operation.IMPORT;
import static org.terracotta.dynamic_config.api.model.Requirement.RESOLVE_EAGERLY;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

/**
 * Parses CLI or config file into a Cluster object, but does not validate the cluster object
 * <p>
 * This class purpose is to be used internally in {@link ClusterFactory}
 */
class ConfigurationParser {

  private final List<Configuration> configurations;
  private final Consumer<Configuration> defaultAddedListener;

  private ConfigurationParser(List<Configuration> configurations, Consumer<Configuration> defaultAddedListener) {
    this.configurations = new ArrayList<>(requireNonNull(configurations));
    this.defaultAddedListener = requireNonNull(defaultAddedListener);
    if (configurations.isEmpty()) {
      throw new IllegalArgumentException("No configuration provided");
    }
  }

  /**
   * @return A parsed cluster object from properties.
   * The cluster object is NOT validated and will need to be validated with the
   * {@link ClusterValidator}
   */
  public Cluster parse() {
    Cluster cluster = new Cluster();

    // Find all the settings that are not define and which require to be resolved eagerly.
    // We cannot have any placeholders now in those settings. They must be previously resolved.
    // Apply them, or their default.
    Stream.of(Setting.values())
        .filter(setting -> setting.requires(RESOLVE_EAGERLY))
        .filter(setting -> setting.isScope(CLUSTER))
        .forEach(setting -> {
          Optional<Configuration> configuration = configurations.stream().filter(cfg -> cfg.getSetting() == setting).findAny();
          if (configuration.isPresent()) {
            configuration.get().apply(cluster);
            // no need to re-apply after
            configurations.remove(configuration.get());
          } else {
            Optional<String> def = setting.getDefaultProperty();
            if (def.isPresent()) {
              if (Substitutor.containsSubstitutionParams(def.get())) {
                throw new IllegalArgumentException("Required setting: '" + setting + "' is missing");
              }
              setting.setProperty(cluster, def.get());
              defaultAddedListener.accept(Configuration.valueOf(setting + "=" + def.get()));
            } else {
              throw new IllegalArgumentException("Required setting: '" + setting + "' is missing");
            }
          }
        });

    // Determine the number of stripes and nodes.
    // This map gives a configuration list per node and stripe
    final TreeMap<Integer, TreeMap<Integer, List<Configuration>>> configurationMap = configurations.stream()
        .filter(configuration -> configuration.getLevel() == NODE)
        .collect(
            groupingBy(Configuration::getStripeId, TreeMap::new,
                groupingBy(Configuration::getNodeId, TreeMap::new,
                    toList())));

    // adds default ids if missing
    {
      // adds first stripe if not defined
      if (configurationMap.isEmpty()) {
        configurationMap.put(1, new TreeMap<>());
      }
      // adds first node if not defined
      if (configurationMap.firstEntry().getValue().isEmpty()) {
        configurationMap.firstEntry().getValue().put(1, new ArrayList<>());
      }
    }

    // ids checks
    {
      // verify the stripe ID numbers
      if (configurationMap.firstKey() != 1) {
        throw new IllegalArgumentException("Stripe ID must start at 1");
      }
      if (configurationMap.lastKey() != configurationMap.size()) {
        throw new IllegalArgumentException("Stripe ID must end at " + configurationMap.size());
      }
      // verify the Node Id numbers
      configurationMap.forEach((stripeId, nodeCounts) -> {
        if (nodeCounts.firstKey() != 1) {
          throw new IllegalArgumentException("Node ID must start at 1 in stripe " + stripeId);
        }
        if (nodeCounts.lastKey() != nodeCounts.size()) {
          throw new IllegalArgumentException("Node ID must end at " + nodeCounts.size() + " in stripe " + stripeId);
        }
      });
    }

    // validate each config line
    {
      configurations.forEach(configuration -> {
        // verify that the line is a supported config
        if (!configuration.getSetting().allows(IMPORT)) {
          throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Reason: now allowed");
        }
        // verify that we only have cluster or node scope
        if (configuration.getLevel() == STRIPE) {
          throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Reason: stripe level configuration not allowed");
        }
        // validate the config object
        configuration.validate(CONFIGURING, IMPORT);
      });
    }

    // build the cluster
    configurationMap.forEach((stripeId, nodeConfigurationsMap) -> {
      Stripe stripe = new Stripe();
      nodeConfigurationsMap.forEach((nodeId, nodeConfigurations) -> {
        // create the node and eagerly initialize the basic fields
        Node node = new Node();
        stripe.addNode(node);
        if (stripe.getNodeCount() != nodeId) {
          throw new AssertionError("Expected node count to be: " + nodeId + " but was: " + stripe.getNodeCount());
        }
        // Find all the settings that are not define and which require to be resolved eagerly.
        // We cannot have any placeholders now in those settings. They must be previously resolved.
        // Apply them, or their default.
        Stream.of(Setting.values())
            .filter(setting -> setting.requires(RESOLVE_EAGERLY))
            .filter(setting -> setting.isScope(NODE))
            .forEach(setting -> {
              Optional<Configuration> configuration = nodeConfigurations.stream().filter(cfg -> cfg.getSetting() == setting).findAny();
              if (configuration.isPresent()) {
                configuration.get().apply(node);
                // no need to re-apply after
                configurations.remove(configuration.get());
              } else {
                Optional<String> def = setting.getDefaultProperty();
                if (def.isPresent()) {
                  if (Substitutor.containsSubstitutionParams(def.get())) {
                    throw new IllegalArgumentException("Required setting: '" + setting + "' is missing for node ID: " + nodeId + " in stripe ID: " + stripeId);
                  }
                  setting.setProperty(node, def.get());
                  defaultAddedListener.accept(Configuration.valueOf("stripe." + stripeId + ".node." + nodeId + "." + setting + "=" + def.get()));
                } else {
                  throw new IllegalArgumentException("Required setting: '" + setting + "' is missing for node ID: " + nodeId + " in stripe ID: " + stripeId);
                }
              }
            });
      });
      cluster.addStripe(stripe);
      if (cluster.getStripeCount() != stripeId) {
        throw new AssertionError("Expected stripe count to be: " + stripeId + " but was: " + cluster.getStripeCount());
      }
    });

    // install all the "settings" inside the cluster
    configurations.forEach(configuration -> configuration.apply(cluster));

    return cluster;
  }

  static Cluster parsePropertyConfiguration(Properties properties, Consumer<Configuration> defaultAddedListener) {
    // Note: node hostname, port and name are all required minimal properties.
    // They are used to identify a node in an exported cluster configuration file
    // and no placeholder resolving can be done client-side
    return new ConfigurationParser(propertiesToConfigurations(properties), defaultAddedListener).parse();
  }

  static Cluster parseCommandLineParameters(Map<Setting, String> userConsoleParameters, IParameterSubstitutor substitutor, Consumer<Configuration> defaultAddedListener) {
    final Properties properties = cliToProperties(userConsoleParameters, substitutor, defaultAddedListener);
    return parsePropertyConfiguration(properties, defaultAddedListener);
  }

  /**
   * Transform some properties from property file to some configuration objects, 1 object representing one line
   */
  private static List<Configuration> propertiesToConfigurations(Properties properties) {
    // transform the config properties in a list of configuration objects
    requireNonNull(properties);
    return properties.entrySet()
        .stream()
        .map(p -> Configuration.valueOf(p.getKey() + "=" + p.getValue()))
        .collect(toList());
  }

  /**
   * Transform the user input CLI into properties
   */
  private static Properties cliToProperties(Map<Setting, String> cliParameters, IParameterSubstitutor parameterSubstitutor, Consumer<Configuration> defaultAddedListener) {
    requireNonNull(cliParameters);

    // transform the user input in CLI in a property config
    Properties properties = new Properties();
    cliParameters.forEach((k, v) -> properties.setProperty(prefixed(k), v));

    // node hostname and name are eagerly resolved in CLI
    Stream.of(Setting.values())
        .filter(setting -> setting.requires(RESOLVE_EAGERLY))
        .forEach(s -> eagerlyResolve(parameterSubstitutor, defaultAddedListener, properties, s));

    // delegate back to the parsing of a config file
    return properties;
  }

  private static void eagerlyResolve(IParameterSubstitutor parameterSubstitutor, Consumer<Configuration> defaultAddedListener, Properties properties, Setting setting) {
    String key = prefixed(setting);
    String set = properties.getProperty(key);
    if (set == null) {
      // We can't have this setting null during Node construction from the client side (e.g. during parsing a config properties
      // file in activate command). Therefore, this logic is here, and not in Node::fillDefaults
      set = parameterSubstitutor.substitute(setting.getDefaultProperty().orElse(""));
      defaultAddedListener.accept(Configuration.valueOf(key + "=" + set));
    } else {
      set = parameterSubstitutor.substitute(set);
    }
    properties.setProperty(key, set);
  }

  private static String prefixed(Setting setting) {
    return setting.isScope(CLUSTER) ? setting.toString() : ("stripe.1.node.1." + setting);
  }
}
