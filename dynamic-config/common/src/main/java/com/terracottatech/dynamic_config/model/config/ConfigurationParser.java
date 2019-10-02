/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.config;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Operation;
import com.terracottatech.dynamic_config.model.Scope;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Parser;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;

import static com.terracottatech.dynamic_config.model.Setting.NODE_HOSTNAME;
import static com.terracottatech.dynamic_config.model.Setting.getAll;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class ConfigurationParser implements Parser<Cluster> {

  private final List<Configuration> configurations;
  private final IParameterSubstitutor paramSubstitutor;

  private ConfigurationParser(IParameterSubstitutor paramSubstitutor, Properties properties) {
    this.paramSubstitutor = requireNonNull(paramSubstitutor);
    this.configurations = toConfigurations(properties);
  }

  /**
   * @return A parsed cluster object from properties.
   * The cluster object is NOT validated and will need to be validated with the
   * {@link com.terracottatech.dynamic_config.model.validation.ClusterValidator}
   */
  @Override
  public Cluster parse() {
    // validate each config line
    configurations.forEach(configuration -> {
      // verify that the line is a supported config
      if (!configuration.getSetting().allowsOperation(Operation.CONFIG)) {
        throw new IllegalArgumentException("Invalid property: " + configuration);
      }
      // verify that we only have cluster or node scope
      if (configuration.getScope() == Scope.STRIPE) {
        throw new IllegalArgumentException("Invalid property: " + configuration);
      }
      // validate the config object
      configuration.validate(Operation.CONFIG, paramSubstitutor);
    });

    checkForMissingClusterWideSettings(configurations);

    // Determine the number of stripes and nodes.
    // This map gives the number of node configurations per node and stripe
    final TreeMap<Integer, TreeMap<Integer, List<Configuration>>> nodeConfigs = configurations.stream()
        .filter(configuration -> configuration.getScope() == Scope.NODE)
        .collect(
            groupingBy(Configuration::getStripeId, TreeMap::new,
                groupingBy(Configuration::getNodeId, TreeMap::new,
                    toList())));

    // verify the Stripe Id numbers
    if (nodeConfigs.firstKey() != 1) {
      throw new IllegalArgumentException("Stripe Id must start at 1");
    }
    if (nodeConfigs.lastKey() != nodeConfigs.size()) {
      throw new IllegalArgumentException("Stripe Id must end at " + nodeConfigs.size());
    }
    // verify the Node Id numbers
    nodeConfigs.forEach((stripeId, nodeCounts) -> {
      if (nodeCounts.firstKey() != 1) {
        throw new IllegalArgumentException("Node ID must start at 1 in stripe " + stripeId);
      }
      if (nodeCounts.lastKey() != nodeCounts.size()) {
        throw new IllegalArgumentException("Node ID must end at " + nodeCounts.size() + " in stripe " + stripeId);
      }
    });

    // ensure that for all the nodes, we have some config, and also that the number of config is the same for every node and matches the expected ones
    for (Map.Entry<Integer, TreeMap<Integer, List<Configuration>>> stripeEntry : nodeConfigs.entrySet()) {
      for (Map.Entry<Integer, List<Configuration>> nodeEntry : stripeEntry.getValue().entrySet()) {
        checkForMissingNodeSettings(stripeEntry.getKey(), nodeEntry.getKey(), nodeEntry.getValue());
      }
    }

    // build the cluster
    Cluster cluster = new Cluster();
    nodeConfigs.forEach((stripeId, nodeCounts) -> {
      Stripe stripe = new Stripe();
      nodeCounts.keySet().forEach(nodeId -> {
        stripe.addNode(new Node().fillDefaults());
        if (stripe.getNodeCount() != nodeId) {
          throw new AssertionError("Expected node count to be: " + nodeId + " but was: " + stripe.getNodeCount());
        }
      });
      cluster.addStripe(stripe);
      if (cluster.getStripeCount() != stripeId) {
        throw new AssertionError("Expected stripe count to be: " + stripeId + " but was: " + cluster.getStripeCount());
      }
    });

    // install all the "settings" inside the cluster
    configurations.forEach(configuration -> configuration.apply(Operation.SET, cluster, paramSubstitutor));

    return cluster;
  }

  private void checkForMissingNodeSettings(int stripeId, int nodeId, List<Configuration> configurations) {
    // validate that the properties contain all settings, not more, no less
    final Set<Setting> actual = configurations.stream()
        .filter(configuration -> configuration.getScope() == Scope.NODE)
        .map(Configuration::getSetting)
        .collect(toSet());
    final Set<Setting> expected = getAll()
        .stream()
        .filter(setting -> setting.allowsOperation(Operation.CONFIG))
        .filter(setting -> setting.isScope(Scope.NODE))
        .collect(toSet());
    if (actual.size() > expected.size()) {
      actual.removeAll(expected);
      throw new IllegalArgumentException("Invalid settings in config file for stripe Id: " + stripeId + " and node Id: " + nodeId + ": " + actual);
    }
    if (actual.size() < expected.size()) {
      expected.removeAll(actual);
      throw new IllegalArgumentException("Missing settings in config file for stripe Id: " + stripeId + " and node Id: " + nodeId + ": " + expected);
    }
  }

  private void checkForMissingClusterWideSettings(List<Configuration> configurations) {
    // validate that the properties contain all settings, not more, no less
    final Set<Setting> actual = configurations.stream()
        .filter(configuration -> configuration.getScope() == Scope.CLUSTER)
        .map(Configuration::getSetting)
        .collect(toSet());
    final Set<Setting> expected = getAll()
        .stream()
        .filter(setting -> setting.allowsOperation(Operation.CONFIG))
        .filter(setting -> setting.isScope(Scope.CLUSTER))
        .collect(toSet());
    if (actual.size() > expected.size()) {
      actual.removeAll(expected);
      throw new IllegalArgumentException("Invalid settings: " + actual);
    }
    if (actual.size() < expected.size()) {
      expected.removeAll(actual);
      throw new IllegalArgumentException("Missing settings: " + expected);
    }
  }

  private static List<Configuration> toConfigurations(Properties properties) {
    // transform the config properties in a list of configuration objects
    requireNonNull(properties);
    return properties.entrySet()
        .stream()
        .map(p -> Configuration.valueOf(p.getKey() + "=" + p.getValue()))
        .collect(toList());
  }

  private static String prefixed(Setting setting) {
    return setting.isScope(Scope.CLUSTER) ? setting.toString() : ("stripe.1.node.1." + setting);
  }

  public static Cluster parsePropertyConfiguration(IParameterSubstitutor substitutor, Properties properties) {
    return new ConfigurationParser(substitutor, properties).parse();
  }

  public static Cluster parseCommandLineParameters(IParameterSubstitutor substitutor, Map<Setting, String> userConsoleParameters) {
    final Properties properties = toProperties(substitutor, userConsoleParameters, (setting, s) -> {
    });
    return parsePropertyConfiguration(substitutor, properties);
  }

  public static Cluster parseCommandLineParameters(IParameterSubstitutor substitutor, Map<Setting, String> userConsoleParameters, BiConsumer<Setting, String> filledPropertyConsumer) {
    final Properties properties = toProperties(substitutor, userConsoleParameters, filledPropertyConsumer);
    return parsePropertyConfiguration(substitutor, properties);
  }

  private static Properties toProperties(IParameterSubstitutor parameterSubstitutor, Map<Setting, String> consoleParameters, BiConsumer<Setting, String> filledPropertyConsumer) {
    requireNonNull(consoleParameters);

    // transform the user input in CLI in a property config
    Properties properties = new Properties();
    consoleParameters.forEach((k, v) -> properties.setProperty(prefixed(k), v));

    if (consoleParameters.get(NODE_HOSTNAME) == null) {
      // We can't hostname null during Node construction from the client side (e.g. during parsing a config properties
      // file in activate command). Therefore, this logic is here, and not in Node::fillDefaults
      String hostname = parameterSubstitutor.substitute(NODE_HOSTNAME.getDefaultValue());
      properties.setProperty(prefixed(NODE_HOSTNAME), hostname);
      filledPropertyConsumer.accept(NODE_HOSTNAME, hostname);
    }

    // put all missing settings with their defaults
    getAll().stream()
        .filter(setting -> setting.allowsOperation(Operation.CONFIG))
        .filter(setting -> !consoleParameters.containsKey(setting))
        .forEach(setting -> {
          if (setting.getDefaultValue() != null) {
            properties.put(prefixed(setting), setting.getDefaultValue());
            filledPropertyConsumer.accept(setting, setting.getDefaultValue());
          } else {
            properties.put(prefixed(setting), ""); // simulate an empty value line in the config file
          }
        });

    // delegate back to the parsing of a config file
    return properties;
  }
}
