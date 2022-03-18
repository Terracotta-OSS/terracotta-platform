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
package org.terracotta.dynamic_config.api.model;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

public class Configuration {

  private static final Collection<String> SETTINGS = Stream.of(Setting.values()).map(Setting::toString).collect(toSet());

  private static final Map<Pattern, BiFunction<String, Matcher, Configuration>> CLUSTER_PATTERNS = new LinkedHashMap<>();
  private static final Map<Pattern, BiFunction<String, Matcher, Configuration>> STRIPE_PATTERNS = new LinkedHashMap<>();
  private static final Map<Pattern, BiFunction<String, Matcher, Configuration>> NODE_PATTERNS = new LinkedHashMap<>();

  private static final String GRP_STRIPE = "stripe\\.(\\d+)";
  private static final String GRP_NODE = "node\\.(\\d+)";
  private static final String SEP = "\\.";
  private static final String NS = "[.:]"; // namespace separator
  private static final String GRP_SETTING = "([a-z\\-]+)";
  private static final String GRP_KEY = "([^=:]+)";
  private static final String ASSIGN = "=";
  private static final String GRP_VALUE = "([^=]+)";

  static {
    // stripe.<index>.node.<index>.<setting>.<key>=<value>
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        matcher.group(5)));
    // stripe.<index>.node.<index>.<setting>=<value>
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        matcher.group(4)));
    // stripe.<index>.<setting>.<key>=<value>
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        matcher.group(4)));
    // stripe.<index>.<setting>=<value>
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        matcher.group(3)));
    // <setting>.<key>=<value>
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        matcher.group(3)));
    // <setting>=<value>
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        null,
        matcher.group(2)));
    // stripe.<index>.node.<index>.<setting>.<key>=
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        ""));
    // stripe.<index>.node.<index>.<setting>=
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        ""));
    // stripe.<index>.<setting>.<key>=
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        ""));
    // stripe.<index>.<setting>=
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        ""));
    // <setting>.<key>=
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        ""));
    // <setting>=
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        null,
        ""));
    // stripe.<index>.node.<index>.<setting>.<key>
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        null));
    // stripe.<index>.node.<index>.<setting>
    NODE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        null));
    // stripe.<index>.<setting>.<key>
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        null));
    // stripe.<index>.<setting>
    STRIPE_PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        null));
    // <setting>.<key>
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        null));
    // <setting>
    CLUSTER_PATTERNS.put(Pattern.compile("^" + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        null,
        null));
  }

  private final String rawInput;
  private final Setting setting;
  private final Scope level;
  private final Integer stripeId;
  private final Integer nodeId;
  private final String key;
  private final String value;

  private Configuration(String rawInput, Setting setting, Scope level, Integer stripeId, Integer nodeId, String key, String value) {
    this.rawInput = requireNonNull(rawInput);
    this.setting = requireNonNull(setting);
    this.level = requireNonNull(level);
    this.stripeId = stripeId;
    this.nodeId = nodeId;
    this.key = key;
    this.value = value == null ? null : value.trim();

    // pre-validate with the real value taken from input
    preValidate();
  }

  public Scope getLevel() {
    return level;
  }

  public int getStripeId() {
    return stripeId;
  }

  public int getNodeId() {
    return nodeId;
  }

  public Setting getSetting() {
    return setting;
  }

  public String getKey() {
    return key;
  }

  public Optional<String> getValue() {
    return value == null || value.isEmpty() ? empty() : Optional.of(value);
  }

  public boolean hasValue() {
    return getValue().isPresent();
  }

  public boolean isSet() {
    return value != null;
  }

  private void preValidate() {
    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting '" + setting + "' is not a map and must not have a key");
    }
    if (stripeId != null && stripeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected stripe ID to be greater than 0");
    }
    if (nodeId != null && nodeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected node ID to be greater than 0");
    }
    if (!setting.allows(level)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting '" + setting + "' does not allow any operation at " + level + " level");
    }
    try {
      setting.validate(key, value, level);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + e.getMessage(), e);
    }
  }

  public void validate(ClusterState clusterState, Operation operation) {
    try {
      setting.validate(key, value, level, clusterState, operation);
    } catch (RuntimeException e) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + e.getMessage(), e);
    }
  }

  /**
   * Check if this configuration is a duplicate of an other configuration. The comparison is reciprocal.
   * <p>
   * Duplicate means:
   * Having the same setting name and scope when the key is null (for non map settings and map settings defined without their key component),
   * or having the same setting name, scope and key.
   * We cannot also mix a configuration that is defined with its key and another one defined without it.
   *
   * @throws IllegalArgumentException if the setting are the same, same scope, same nodes to apply to, they are a map but their definition does not allows to check for duplication
   */
  public boolean duplicates(Configuration other) throws IllegalArgumentException {
    if (setting != other.setting) {
      // not the same setting
      return false;
    }
    if (level != other.level) {
      // same setting, but different scopes
      return false;
    }
    if (!Objects.equals(stripeId, other.stripeId) || !Objects.equals(nodeId, other.nodeId)) {
      // same setting, same scope, but they apply on different nodes
      return false;
    }
    // here, we have the same setting, same scope, and same nodes to apply to
    if (!setting.isMap()) {
      // if the setting is not a map then this is a duplicate
      return true;
    }
    // here, we have the same setting, same scope, and same nodes to apply to, and the setting is a map
    if (key != null && other.key != null && Objects.equals(key, other.key)) {
      // if the keys are equals and non null, it means this is a map setting on the same key
      return true;
    }
    if (key != null && other.key != null && !Objects.equals(key, other.key)) {
      // if the keys are not equals and non null, it means this is a map setting on different keys
      return false;
    }
    // here, we have the same setting, same scope, and same nodes to apply to, and the setting is a map
    if (key == null && other.key == null && Objects.equals(value, other.value)) {
      // if the keys are null, we can tell this is a duplicate if the values are the same
      return true;
    }
    // here, we have the same setting, same scope, and same nodes to apply to, and the setting are a map,
    // but we have either null keys, or a mix, with different values
    // so we cannot tell if there are some duplication
    throw new IllegalArgumentException("Incompatible or duplicate configurations: " + this + " and " + other);
  }

  /**
   * Check if this configuration "pattern" supports the given property config
   */
  public boolean matchConfigPropertyKey(String key) {
    Configuration configPropertyKey = Configuration.valueOf(key);
    if (configPropertyKey.setting != this.setting) {
      return false;
    }
    if (this.stripeId != null && !Objects.equals(this.stripeId, configPropertyKey.stripeId)) {
      // we want to match a node or stripe scope
      return false;
    }
    if (this.nodeId != null && !Objects.equals(this.nodeId, configPropertyKey.nodeId)) {
      // we want to match a specific node
      return false;
    }
    if (this.setting.isMap() && this.key != null && !Objects.equals(this.key, configPropertyKey.key)) {
      // case of a map, where we want to match a specific key
      return false;
    }
    if (this.setting.isMap() && this.key == null && configPropertyKey.key != null) {
      // case of a map, where we want to match a specific key
      return false;
    }
    return true;
  }

  public Collection<? extends PropertyHolder> apply(PropertyHolder propertyHolder) {
    Collection<? extends PropertyHolder> targets = findTargets(propertyHolder).collect(toList());
    targets.forEach(target -> setting.setProperty(target, key, value));
    return targets;
  }

  @Override
  public int hashCode() {
    return rawInput.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof Configuration)) return false;
    Configuration that = (Configuration) o;
    return rawInput.equals(that.rawInput);
  }

  @Override
  public String toString() {
    return rawInput;
  }

  /**
   * This method returns all objects targeted by this configuration change depending
   * on the argument and the type of change and its level (namespace).
   * <pre>
   * - If this change's setting scope is NODE, then "Node" will be returned (changing a setting for a node)
   * - If this change's setting scope is STRIPE, then "Stripe" will be returned (changing a setting for a stripe)
   * - If this change's setting scope is CLUSTER, then "Cluster" will be returned (changing a setting for a cluster)
   * - If this change's level is CLUSTER, then:
   *   * from must be "Cluster"
   * - If this change's level is STRIPE, then:
   *   * from must be "Cluster" or "Stripe"
   * - If this change's level is NODE, then:
   *   * from must be "Cluster" or "Stripe" or "Node
   * </pre>
   */
  public Stream<? extends PropertyHolder> findTargets(PropertyHolder from) {
    Stream<? extends PropertyHolder> targets;
    switch (this.level) {

      // cluster-wide namespace
      // can only target cluster, stripe and node
      case CLUSTER:
        // we can only apply it on a parameter of type
        switch (from.getScope()) {
          case CLUSTER:
          case STRIPE:
          case NODE: {
            targets = Stream.concat(Stream.of(from), from.descendants());
            break;
          }
          default:
            throw new AssertionError(from.getScope());
        }
        break;

      // stripe-wide namespace
      // can only target stripes and nodes
      case STRIPE:
        // we can only apply it on a parameter of type cluster and stripe
        switch (from.getScope()) {
          case CLUSTER: {
            Cluster cluster = (Cluster) from;
            Stripe stripe = cluster.getStripe(stripeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid stripe ID: " + stripeId + ". Cluster contains: " + cluster.getStripeCount() + " stripe(s)"));
            targets = Stream.concat(Stream.of(stripe), stripe.descendants());
            break;
          }
          case STRIPE:
          case NODE: {
            targets = Stream.concat(Stream.of(from), from.descendants());
            break;
          }
          default:
            throw new AssertionError(from.getScope());
        }
        break;

      // node namespace
      // can only target nodes
      case NODE:
        switch (from.getScope()) {
          case CLUSTER: {
            Cluster cluster = (Cluster) from;
            Stripe stripe = cluster.getStripe(stripeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid stripe ID: " + stripeId + ". Cluster contains: " + cluster.getStripeCount() + " stripe(s)"));
            Node node = getNode(stripe, nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid node ID: " + nodeId + ". Stripe ID: " + stripeId + " contains: " + stripe.getNodeCount() + " node(s)"));
            targets = Stream.concat(Stream.of(node), node.descendants());
            break;
          }
          case STRIPE: {
            Stripe stripe = (Stripe) from;
            Node node = getNode(stripe, nodeId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid node ID: " + nodeId + ". Stripe ID: " + stripeId + " contains: " + stripe.getNodeCount() + " node(s)"));
            targets = Stream.concat(Stream.of(node), node.descendants());
            break;
          }
          case NODE: {
            targets = Stream.concat(Stream.of(from), from.descendants());
            break;
          }
          default:
            throw new AssertionError(from.getScope());
        }
        break;

      default:
        throw new AssertionError(this.level);
    }

    // ensure to filter the targets to only chose the roght object type (node, stripe or cluster)
    // depending on which type of object this setting applies to
    return targets.filter(ph -> ph.getScope() == getSetting().getScope());
  }

  public List<Configuration> expand() {
    // if the config is not a map or is a map but with one key, we do not have a compound value like:
    // - data-dirs=foo:bar
    // - data-dirs=foo:bar,foo2:bar2
    if (!setting.isMap() || key != null || !hasValue()) {
      return Collections.singletonList(this);
    }
    String[] keyValue = rawInput.split("=", 2);
    return Stream.of(keyValue[1].split(","))
        .filter(s -> !s.trim().isEmpty()) // in case user sends a lot of commas: data-dirs=,foo:bar,,foo2:bar2,,
        .map(s -> {
          String[] nameProperty = s.trim().split(":", 2);
          if (nameProperty.length != 2) {
            // we always expect a key and value for maps
            throw new IllegalArgumentException("Invalid input: " + value);
          }
          return keyValue[0] + "." + nameProperty[0] + "=" + nameProperty[1];
        })
        .distinct() // in case user sends: data-dirs=foo:bar,foo:bar
        .map(Configuration::valueOf)
        .collect(toList());
  }

  public static Configuration valueOf(String key, String value) {
    return valueOf(key + "=" + (value == null ? "" : value));
  }

  /**
   * Parses configuration inputs such as:
   * <pre>
   *  get -c stripe.1.node.1.offheap-resources.main
   *  get -c stripe.1.offheap-resources.main
   *  get -c offheap-resources.main
   *
   *  get -c stripe.1.node.1.offheap-resources
   *  get -c stripe.1.offheap-resources
   *  get -c offheap-resources
   *
   *  set -c stripe.1.node.1.offheap-resources.main=1G
   *  set -c stripe.1.offheap-resources.main=1G
   *  set -c offheap-resources.main=1G
   *
   *  get -c stripe.1.node.1.backup-dir
   *  get -c stripe.1.backup-dir
   *  get -c backup-dir
   *
   *  set -c stripe.1.node.1.backup-dir=foo/bar
   *  set -c stripe.1.backup-dir=foo/bar
   *  set -c backup-dir=foo/bar
   * </pre>
   */
  public static Configuration valueOf(String input) {
    requireNonNull(input);

    input = input.trim();

    Integer stripeId = null;
    Integer nodeId = null;
    Setting setting = null;

    try (Scanner scanner = new Scanner(input).useDelimiter("[.:=]")) {
      while (scanner.hasNext() && setting == null) {
        String next = scanner.next();
        switch (next) {
          case "stripe": {
            if (stripeId != null) {
              throw new IllegalArgumentException("Invalid input: '" + input + "'");
            }
            stripeId = scanner.nextInt(10);
            break;
          }
          case "node": {
            if (nodeId != null || stripeId == null) {
              throw new IllegalArgumentException("Invalid input: '" + input + "'");
            }
            nodeId = scanner.nextInt(10);
            break;
          }
          default: {
            if (SETTINGS.contains(next)) {
              setting = Setting.fromName(next);
            } else {
              throw new IllegalArgumentException("Invalid input: '" + input + "'. Reason: Invalid setting name: '" + next + "'");
            }
          }
        }
      }
    } catch (NoSuchElementException e) {
      throw new IllegalArgumentException("Invalid input: '" + input + "'");
    }

    if (setting == null) {
      throw new IllegalArgumentException("Invalid input: '" + input + "'. Reason: valid setting name not found");
    }

    if (stripeId != null && nodeId != null) {
      for (Map.Entry<Pattern, BiFunction<String, Matcher, Configuration>> entry : NODE_PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(input);
        if (matcher.matches()) {
          return entry.getValue().apply(input, matcher);
        }
      }
    }

    if (stripeId != null && nodeId == null) {
      for (Map.Entry<Pattern, BiFunction<String, Matcher, Configuration>> entry : STRIPE_PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(input);
        if (matcher.matches()) {
          return entry.getValue().apply(input, matcher);
        }
      }
    }

    if (stripeId == null) {
      for (Map.Entry<Pattern, BiFunction<String, Matcher, Configuration>> entry : CLUSTER_PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(input);
        if (matcher.matches()) {
          return entry.getValue().apply(input, matcher);
        }
      }
    }

    throw new IllegalArgumentException("Invalid input: '" + input + "'");
  }

  private static Optional<Node> getNode(Stripe stripe, int nodeId) {
    if (nodeId < 1) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId);
    }
    if (nodeId > stripe.getNodeCount()) {
      return Optional.empty();
    }
    return Optional.of(stripe.getNodes().get(nodeId - 1));
  }
}
