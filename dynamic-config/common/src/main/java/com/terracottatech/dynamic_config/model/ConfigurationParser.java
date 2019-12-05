/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Parser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.model.Node.newDefaultNode;
import static com.terracottatech.dynamic_config.model.Operation.CONFIG;
import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.Scope.STRIPE;
import static com.terracottatech.dynamic_config.model.Setting.NODE_HOSTNAME;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Parses CLI or config file into a Cluster object, but does not validate the cluster object
 * <p>
 * This class purpose is to be used internally in {@link ClusterFactory}
 */
class ConfigurationParser implements Parser<Cluster> {

  private final List<Configuration> configurations;
  private final Consumer<Configuration> defaultAddedListener;
  private final IParameterSubstitutor paramSubstitutor;

  private ConfigurationParser(IParameterSubstitutor paramSubstitutor, List<Configuration> configurations, Consumer<Configuration> defaultAddedListener) {
    this.paramSubstitutor = requireNonNull(paramSubstitutor);
    this.configurations = new ArrayList<>(requireNonNull(configurations));
    this.defaultAddedListener = requireNonNull(defaultAddedListener);
  }

  /**
   * @return A parsed cluster object from properties.
   * The cluster object is NOT validated and will need to be validated with the
   * {@link ClusterValidator}
   */
  @Override
  public Cluster parse() {
    // adds missing configurations
    {
      final Collection<Setting> defined = configurations.stream().map(Configuration::getSetting).collect(toSet());
      // search amongst all the settings allowed to be set at configuration level,
      // those that are scoped CLUSTER and that have not been defined by the user
      Stream.of(Setting.values())
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
      final Set<Setting> expected = Stream.of(Setting.values())
          .filter(setting -> setting.allowsOperation(CONFIG))
          .filter(setting -> setting.isScope(CLUSTER))
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

        // adds missing node configurations
        {
          final Collection<Setting> defined = nodeConfigurations.stream().map(Configuration::getSetting).collect(toSet());
          // search amongst all the settings allowed to be set at configuration level for a node,
          // those that are scoped NODE and that have not been defined by the user
          Stream.of(Setting.values())
              .filter(setting -> setting.isScope(NODE))
              .filter(setting -> setting.allowsOperation(CONFIG))
              .filter(setting -> !defined.contains(setting))
              .map(setting -> Configuration.valueOf(stripeId, nodeId, setting))
              .forEach(configuration -> {
                // we add the missing configuration to both the global list, plus the temporary list within the map used for validation
                nodeConfigurations.add(configuration);
                configurations.add(configuration);
                defaultAddedListener.accept(configuration);
              });
        }

        // Supplementary validation that ensures that our configuration list contains all the settings for the node, no more, no less
        {
          final Set<Setting> actual = nodeConfigurations.stream()
              .filter(configuration -> configuration.getScope() == NODE)
              .map(Configuration::getSetting)
              .collect(toSet());
          final Set<Setting> expected = Stream.of(Setting.values())
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
          throw new IllegalArgumentException("Invalid property: " + configuration);
        }
        // verify that we only have cluster or node scope
        if (configuration.getScope() == STRIPE) {
          throw new IllegalArgumentException("Invalid property: " + configuration);
        }
        // validate the config object
        configuration.validate(CONFIG);
        // ensure that properties requiring an eager resolve are resolved
        if (configuration.getSetting().requiresEagerSubstitution() && IParameterSubstitutor.containsSubstitutionParams(configuration.getValue())) {
          throw new IllegalArgumentException("Invalid property: " + configuration + ". Placeholders are not allowed.");
        }
      });
    }

    // build the cluster
    Cluster cluster = new Cluster();
    configurationMap.forEach((stripeId, nodeCounts) -> {
      Stripe stripe = new Stripe();
      nodeCounts.keySet().forEach(nodeId -> {
        stripe.addNode(newDefaultNode(null));
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
    configurations.forEach(configuration -> configuration.apply(cluster));

    return cluster;
  }

  static Cluster parsePropertyConfiguration(IParameterSubstitutor substitutor, Properties properties) {
    return parsePropertyConfiguration(substitutor, properties, configuration -> {
    });
  }

  static Cluster parsePropertyConfiguration(IParameterSubstitutor substitutor, Properties properties, Consumer<Configuration> defaultAddedListener) {
    return new ConfigurationParser(substitutor, propertiesToConfigurations(properties), defaultAddedListener).parse();
  }

  static Cluster parseCommandLineParameters(IParameterSubstitutor substitutor, Map<Setting, String> userConsoleParameters) {
    return parseCommandLineParameters(substitutor, userConsoleParameters, configuration -> {
    });
  }

  static Cluster parseCommandLineParameters(IParameterSubstitutor substitutor, Map<Setting, String> userConsoleParameters, Consumer<Configuration> defaultAddedListener) {
    final Properties properties = cliToProperties(substitutor, userConsoleParameters, defaultAddedListener);
    return parsePropertyConfiguration(substitutor, properties, defaultAddedListener);
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
  private static Properties cliToProperties(IParameterSubstitutor parameterSubstitutor, Map<Setting, String> consoleParameters, Consumer<Configuration> defaultAddedListener) {
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
      properties.setProperty(key, hostname);
      defaultAddedListener.accept(Configuration.valueOf(key + "=" + hostname));
    } else {
                hostname = parameterSubstitutor.substitute(hostname);
      properties.setProperty(key, hostname);
      consoleParameters.put(NODE_HOSTNAME, parameterSubstitutor.substitute(hostname));
    }

    // delegate back to the parsing of a config file
    return properties;
  }

  private static String prefixed(Setting setting) {
    return setting.isScope(CLUSTER) ? setting.toString() : ("stripe.1.node.1." + setting);
  }
}
