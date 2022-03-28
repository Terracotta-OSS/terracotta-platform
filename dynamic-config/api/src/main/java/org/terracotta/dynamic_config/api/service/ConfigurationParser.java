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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.api.model.Operation.CONFIG;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_HOSTNAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_NAME;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_PORT;
import static org.terracotta.dynamic_config.api.model.Setting.values;

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
    if (configurations.stream().noneMatch(configuration -> configuration.getSetting() == NODE_HOSTNAME)) {
      throw new IllegalArgumentException(NODE_HOSTNAME + " is missing");
    }
    if (configurations.stream().anyMatch(configuration -> configuration.getSetting() == NODE_HOSTNAME && IParameterSubstitutor.containsSubstitutionParams(configuration.getValue()))) {
      throw new IllegalArgumentException(NODE_HOSTNAME + " cannot contain any placeholders");
    }
  }

  /**
   * @return A parsed cluster object from properties.
   * The cluster object is NOT validated and will need to be validated with the
   * {@link ClusterValidator}
   */
  public Cluster parse() {
    // adds missing configurations
    {
      final Collection<Setting> defined = configurations.stream().map(Configuration::getSetting).collect(toSet());
      // search amongst all the settings allowed to be set at configuration level,
      // those that are scoped CLUSTER and that have not been defined by the user
      Stream.of(values())
          .filter(setting -> setting.isScope(CLUSTER))
          .filter(setting -> setting.allowsOperation(CONFIG))
          .filter(setting -> !defined.contains(setting))
          .map(Configuration::valueOf)
          .forEach(configuration -> {
            configurations.add(configuration);
            defaultAddedListener.accept(configuration);
          });
    }

    // Supplementary validation that ensures that our configuration list contains all the settings for the cluster, no more, no less
    {
      final Set<Setting> actual = configurations.stream()
          .filter(configuration -> configuration.getScope() == CLUSTER)
          .map(Configuration::getSetting)
          .collect(toSet());
      final Set<Setting> expected = Stream.of(values())
          .filter(setting -> setting.allowsOperation(CONFIG))
          .filter(setting -> setting.isScope(CLUSTER))
          .collect(toSet());
      if (actual.size() > expected.size()) {
        actual.removeAll(expected);
        throw new IllegalArgumentException("Invalid settings found at cluster level: " + actual.stream().map(Objects::toString).collect(Collectors.joining(", ")));
      }
      if (actual.size() < expected.size()) {
        expected.removeAll(actual);
        throw new IllegalArgumentException("Missing settings at cluster level: " + expected.stream().map(Objects::toString).collect(Collectors.joining(", ")));
      }
    }

    // Determine the number of stripes and nodes.
    // This map gives a configuration list per node and stripe
    final TreeMap<Integer, TreeMap<Integer, List<Configuration>>> configurationMap = configurations.stream()
        .filter(configuration -> configuration.getScope() == NODE)
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

    for (Map.Entry<Integer, TreeMap<Integer, List<Configuration>>> stripeEntry : configurationMap.entrySet()) {
      for (Map.Entry<Integer, List<Configuration>> nodeEntry : stripeEntry.getValue().entrySet()) {
        int stripeId = stripeEntry.getKey();
        int nodeId = nodeEntry.getKey();
        List<Configuration> nodeConfigurations = nodeEntry.getValue();

        // add missing node configurations
        {
          final Collection<Setting> defined = nodeConfigurations.stream().map(Configuration::getSetting).collect(toSet());
          // search amongst all the settings allowed to be set at configuration level for a node,
          // those that are scoped NODE and that have not been defined by the user
          Stream.of(values())
              .filter(setting -> setting.isScope(NODE))
              .filter(setting -> setting.allowsOperation(CONFIG))
              .filter(setting -> !defined.contains(setting))
              .map(setting -> Configuration.valueOf(setting, stripeId, nodeId))
              .forEach(configuration -> {
                // we add the missing configuration to both the global list, plus the temporary list within the map used for validation
                nodeConfigurations.add(configuration);
                configurations.add(configuration);
                defaultAddedListener.accept(configuration);
              });
        }

        // Supplementary validation that ensures that our configuration list contains all the settings for the node, no more, no less
        // Note: we should in theory never throw here: this is a safe check to prevent any programming error
        {
          final Set<Setting> actual = nodeConfigurations.stream()
              .filter(configuration -> configuration.getScope() == NODE)
              .map(Configuration::getSetting)
              .collect(toSet());
          final Set<Setting> expected = Stream.of(values())
              .filter(setting -> setting.allowsOperation(CONFIG))
              .filter(setting -> setting.isScope(NODE))
              .collect(toSet());
          if (actual.size() > expected.size()) {
            actual.removeAll(expected);
            throw new IllegalArgumentException("Invalid settings in config file for stripe ID: " + stripeId + " and node ID: " + nodeId + ": " + actual);
          }
          if (actual.size() < expected.size()) {
            expected.removeAll(actual);
            throw new IllegalArgumentException("Missing settings in config file for stripe ID: " + stripeId + " and node ID: " + nodeId + ": " + expected);
          }
        }
      }
    }

    // validate each config line
    {
      configurations.forEach(configuration -> {
        // verify that the line is a supported config
        if (!configuration.getSetting().allowsOperation(CONFIG)) {
          throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Reason: now allowed");
        }
        // verify that we only have cluster or node scope
        if (configuration.getScope() == STRIPE) {
          throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Reason: stripe level configuration not allowed");
        }
        // validate the config object
        configuration.validate(CONFIG);
        // ensure that properties requiring an eager resolve are resolved
        if (configuration.getSetting().requiresEagerSubstitution() && IParameterSubstitutor.containsSubstitutionParams(configuration.getValue())) {
          throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Placeholders are not allowed");
        }
      });
    }

    // build the cluster
    Cluster cluster = Cluster.newCluster();
    configurationMap.forEach((stripeId, nodeCounts) -> {
      Stripe stripe = new Stripe();
      nodeCounts.keySet().forEach(nodeId -> {
        // create the node and eagerly initialize the basic fields
        Node node = Node.empty();
        stripe.addNode(node);
        if (stripe.getNodeCount() != nodeId) {
          throw new AssertionError("Expected node count to be: " + nodeId + " but was: " + stripe.getNodeCount());
        }
        Stream.of(NODE_NAME, NODE_HOSTNAME, NODE_PORT).map(setting -> configurations.stream()
            .filter(configuration -> configuration.getSetting() == setting
                && configuration.getScope() == NODE
                && configuration.getStripeId() == stripeId
                && configuration.getNodeId() == nodeId)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Required setting missing: '" + setting + "' for node ID: " + nodeId + " in stripe ID: " + stripeId)))
            .forEach(o -> o.apply(node));
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
  private static Properties cliToProperties(Map<Setting, String> consoleParameters, IParameterSubstitutor parameterSubstitutor, Consumer<Configuration> defaultAddedListener) {
    requireNonNull(consoleParameters);

    // transform the user input in CLI in a property config
    Properties properties = new Properties();
    consoleParameters.forEach((k, v) -> properties.setProperty(prefixed(k), v));

    String key = prefixed(NODE_HOSTNAME);
    String hostname = consoleParameters.get(NODE_HOSTNAME);
    if (hostname == null) {
      // We can't have hostname null during Node construction from the client side (e.g. during parsing a config properties
      // file in activate command). Therefore, this logic is here, and not in Node::fillDefaults
      hostname = parameterSubstitutor.substitute(NODE_HOSTNAME.getDefaultValue());
      defaultAddedListener.accept(Configuration.valueOf(key + "=" + hostname));
    } else {
      hostname = parameterSubstitutor.substitute(hostname);
    }
    properties.setProperty(key, hostname);

    // delegate back to the parsing of a config file
    return properties;
  }

  private static String prefixed(Setting setting) {
    return setting.isScope(CLUSTER) ? setting.toString() : ("stripe.1.node.1." + setting);
  }
}
