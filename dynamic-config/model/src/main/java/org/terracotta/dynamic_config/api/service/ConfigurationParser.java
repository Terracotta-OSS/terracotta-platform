/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2024, 2025
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
import org.terracotta.dynamic_config.api.model.PropertyHolder;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.Substitutor;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toCollection;
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
 * <p>
 * IMPORTANT: this class is not thread-safe. It has to be constructed and used (parse) by the same thread.
 */
final class ConfigurationParser {

  private final List<Configuration> configurations;
  private final Version version;
  private final Consumer<Configuration> defaultAddedListener;

  private ConfigurationParser(List<Configuration> configurations, Version version, Consumer<Configuration> defaultAddedListener) {
    this.configurations = new ArrayList<>(requireNonNull(configurations));
    this.version = version;
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
    // Determine the number of stripes and nodes.
    // This map gives a configuration list per node and stripe
    final TreeMap<Integer, TreeSet<Integer>> ids = configurations.stream()
        .filter(configuration -> configuration.getLevel() == NODE)
        .collect(
            groupingBy(Configuration::getStripeId, TreeMap::new,
                mapping(Configuration::getNodeId, toCollection(TreeSet::new))));

    // adds default ids if missing
    {
      // adds first stripe if not defined
      if (ids.isEmpty()) {
        ids.put(1, new TreeSet<>());
      }
      // adds first node if not defined
      if (ids.firstEntry().getValue().isEmpty()) {
        ids.firstEntry().getValue().add(1);
      }
    }

    // ids checks
    {
      // verify the stripe ID numbers
      if (ids.firstKey() != 1) {
        throw new IllegalArgumentException("Stripe ID must start at 1");
      }
      if (ids.lastKey() != ids.size()) {
        throw new IllegalArgumentException("Stripe ID must end at " + ids.size());
      }
      // verify the Node Id numbers
      ids.forEach((stripeId, nodesIds) -> {
        if (nodesIds.first() != 1) {
          throw new IllegalArgumentException("Node ID must start at 1 in stripe " + stripeId);
        }
        if (nodesIds.last() != nodesIds.size()) {
          throw new IllegalArgumentException("Node ID must end at " + nodesIds.size() + " in stripe " + stripeId);
        }
      });
    }

    validateConfigurations();

    // build the cluster
    Cluster cluster = new Cluster();

    eagerlyApplySetting(
        cluster,
        cfg -> true,
        setting -> defaultAddedListener.accept(Configuration.valueOf(setting + "=" + setting.getDefaultProperty().get())),
        setting -> {
          throw new IllegalArgumentException("Required setting: '" + setting + "' is missing");
        });

    ids.forEach((stripeId, nodeIds) -> {

      // create stripe
      Stripe stripe = new Stripe();
      cluster.addStripe(stripe);
      if (cluster.getStripeCount() != stripeId) {
        throw new AssertionError("Expected stripe count to be: " + stripeId + " but was: " + cluster.getStripeCount());
      }

      eagerlyApplySetting(
          stripe,
          cfg -> cfg.getStripeId() == stripeId,
          setting -> defaultAddedListener.accept(Configuration.valueOf("stripe." + stripeId + "." + setting + "=" + setting.getDefaultProperty().get())),
          setting -> {
            throw new IllegalArgumentException("Required setting: '" + setting + "' is missing for stripe ID: " + stripeId);
          });

      nodeIds.forEach(nodeId -> {

        // create the node and eagerly initialize the basic fields
        Node node = new Node();
        stripe.addNode(node);
        if (stripe.getNodeCount() != nodeId) {
          throw new AssertionError("Expected node count to be: " + nodeId + " but was: " + stripe.getNodeCount());
        }

        eagerlyApplySetting(
            node,
            cfg -> cfg.getStripeId() == stripeId && cfg.getNodeId() == nodeId,
            setting -> defaultAddedListener.accept(Configuration.valueOf("stripe." + stripeId + ".node." + nodeId + "." + setting + "=" + setting.getDefaultProperty().get())),
            setting -> {
              throw new IllegalArgumentException("Required setting: '" + setting + "' is missing for node ID: " + nodeId + " in stripe ID: " + stripeId);
            });
      });
    });

    // install all the remaining settings inside the model
    configurations.stream()
        .filter(configuration -> version.amongst(configuration.getSetting().getVersions()))
        .forEach(configuration -> {
          Setting setting = configuration.getSetting();
          if (setting.isMap()) {
            // if the setting is a map, when we parse an expanded config file like:
            // stripe.1.node.1.data-dirs.bar=%H/tc1/bar
            // stripe.1.node.1.data-dirs.foo=%H/tc1/foo
            // we need to first "unset" the map.
            // This is required because before that, the user could have used the default.
            // So when setting a property expanded, this is an "addition". So the default value will stay (i.e. set command).
            // But when parsing a config file, we can assume we do not want the default since all the setting are describe
            configuration.findTargets(cluster).forEach(o -> {
              // for each object in the model targeted by this config,
              // we check if a value has been previously set. If not,
              // we set the value to empty
              if (!setting.getProperty(o).isPresent()) {
                setting.setProperty(o, ""); // for a map, will explicitly set to empty (and will clear the default value)
              }
            });
          }
          configuration.apply(cluster);
        });

    return cluster;
  }

  private void validateConfigurations() {
    configurations.forEach(configuration -> {
      // verify that the line is a supported config
      if (!configuration.getSetting().allows(IMPORT)) {
        throw new IllegalArgumentException("Invalid input: '" + configuration + "'. Reason: now allowed");
      }
      // validate the config object
      configuration.validate(CONFIGURING, IMPORT);
    });
  }

  private void eagerlyApplySetting(PropertyHolder o, Predicate<Configuration> filter, Consumer<Setting> onDefaultAdded, Consumer<Setting> onError) {
    Map<Setting, List<Configuration>> configs = configurations.stream()
        .filter(cfg -> cfg.getSetting().isScope(o.getScope()))
        .filter(cfg -> cfg.getLevel() == o.getScope())
        .filter(filter)
        .collect(groupingBy(Configuration::getSetting));

    Stream.of(Setting.values())
        .filter(setting -> version.amongst(setting.getVersions()))
        .filter(setting -> setting.requires(RESOLVE_EAGERLY))
        .filter(setting -> setting.isScope(o.getScope()))
        .forEach(setting -> {
          List<Configuration> defined = configs.getOrDefault(setting, emptyList());
          if (!defined.isEmpty()) {
            defined.forEach(c -> c.apply(o));
            // no need to re-apply after
            configurations.removeAll(defined);
          } else if (!setting.getProperty(o).isPresent()) {
            Optional<String> def = setting.getDefaultProperty();
            if (def.isPresent()) {
              if (Substitutor.containsSubstitutionParams(def.get())) {
                onError.accept(setting);
              } else {
                setting.setProperty(o, def.get());
                onDefaultAdded.accept(setting);
              }
            } else {
              onError.accept(setting);
            }
          }
        });
  }

  static Cluster parsePropertyConfiguration(Properties properties, Version version, Consumer<Configuration> defaultAddedListener) {
    // Note: node hostname, port and name are all required minimal properties.
    // They are used to identify a node in an exported cluster configuration file
    // and no placeholder resolving can be done client-side
    return new ConfigurationParser(propertiesToConfigurations(properties), version, defaultAddedListener).parse();
  }

  static Cluster parseCommandLineParameters(Map<Setting, String> userConsoleParameters, IParameterSubstitutor substitutor, Consumer<Configuration> defaultAddedListener) {
    final Properties properties = cliToProperties(userConsoleParameters, substitutor, defaultAddedListener);
    return parsePropertyConfiguration(properties, Version.CURRENT, defaultAddedListener);
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
    if (set == null) {
      throw new AssertionError(key + " is null: bad mocking ? Default value was: " + setting.getDefaultProperty().orElse(""));
    }
    properties.setProperty(key, set);
  }

  private static String prefixed(Setting setting) {
    return setting.isScope(CLUSTER) ? setting.toString() : setting.isScope(STRIPE) ? ("stripe.1." + setting) : ("stripe.1.node.1." + setting);
  }
}
