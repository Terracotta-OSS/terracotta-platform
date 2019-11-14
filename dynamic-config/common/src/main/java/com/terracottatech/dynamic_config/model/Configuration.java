/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

import com.terracottatech.dynamic_config.util.IParameterSubstitutor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.model.Operation.UNSET;
import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.Scope.STRIPE;
import static com.terracottatech.dynamic_config.model.Setting.CLUSTER_NAME;
import static java.util.Objects.requireNonNull;

public class Configuration {

  private static final Map<Pattern, BiFunction<String, Matcher, Configuration>> PATTERNS = new LinkedHashMap<>();

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
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        matcher.group(5)));
    // stripe.<index>.node.<index>.<setting>=<value>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        matcher.group(4)));
    // stripe.<index>.<setting>.<key>=<value>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        matcher.group(4)));
    // stripe.<index>.<setting>=<value>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        matcher.group(3)));
    // <setting>.<key>=<value>
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        matcher.group(3)));
    // <setting>=<value>
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        null,
        matcher.group(2)));
    // stripe.<index>.node.<index>.<setting>.<key>=
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        null));
    // stripe.<index>.node.<index>.<setting>=
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        null));
    // stripe.<index>.<setting>.<key>=
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        null));
    // stripe.<index>.<setting>=
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        null));
    // <setting>.<key>=
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        null));
    // <setting>=
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        null,
        null));
    // stripe.<index>.node.<index>.<setting>.<key>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        matcher.group(4),
        null));
    // stripe.<index>.node.<index>.<setting>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + SEP + GRP_NODE + NS + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(3)),
        NODE,
        Integer.parseInt(matcher.group(1)),
        Integer.parseInt(matcher.group(2)),
        null,
        null));
    // stripe.<index>.<setting>.<key>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        matcher.group(3),
        null));
    // stripe.<index>.<setting>
    PATTERNS.put(Pattern.compile("^" + GRP_STRIPE + NS + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(2)),
        STRIPE,
        Integer.parseInt(matcher.group(1)),
        null,
        null,
        null));
    // <setting>.<key>
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> new Configuration(
        input,
        Setting.fromName(matcher.group(1)),
        CLUSTER,
        null,
        null,
        matcher.group(2),
        null));
    // <setting>
    PATTERNS.put(Pattern.compile("^" + GRP_SETTING + "$"), (input, matcher) -> new Configuration(
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
  private final Scope scope;
  private final Integer stripeId;
  private final Integer nodeId;
  private final String key;
  private final String value;

  private Configuration(String rawInput, Setting setting, Scope scope, Integer stripeId, Integer nodeId, String key, String value) {
    this.rawInput = requireNonNull(rawInput);
    this.setting = requireNonNull(setting);
    this.scope = requireNonNull(scope);
    this.stripeId = stripeId;
    this.nodeId = nodeId;
    this.key = key;
    this.value = value;

    preValidate();
  }

  public Scope getScope() {
    return scope;
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

  public String getValue() {
    return value;
  }

  private void preValidate() {
    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " is not a map and must not have a key name");
    }
    if (stripeId != null && stripeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected stripe ID to be greater than 0");
    }
    if (nodeId != null && nodeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected node ID to be greater than 0");
    }
    if (!setting.allowsOperationsWithScope(scope)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " does not allow scope " + scope);
    }
    if (setting.isScope(CLUSTER) && (stripeId != null || nodeId != null)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " is a cluster setting not at a stripe or node level");
    }
  }

  public void validate(Operation operation, IParameterSubstitutor substitutor) {
    if (value != null && !substitutor.containsSubstitutionParams(value)) {
      try {
        setting.validate(key, value);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + e.getMessage(), e);
      }
    }
    if (!setting.allowsOperation(operation)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " does not allow operation " + operation);
    }
    switch (operation) {
      case GET:
      case UNSET:
        if (value != null) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " must not have a value");
        }
        break;
      case SET:
        if (value == null) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " requires a value");
        }
        break;
      case CONFIG:
        if (value == null && !setting.allowsOperation(UNSET) && setting.getDefaultValue() == null) {
          if (setting != CLUSTER_NAME) {
            // TODO: find a better way to move this special check somewhere else. Cluster name is a special case where the value is optional and cannot be unset
            throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " requires a value");
          }
        }
        break;
      default:
        throw new AssertionError(operation);
    }
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

  /**
   * Apply the value in this configuration in the given cluster
   */
  public void apply(Cluster cluster, IParameterSubstitutor substitutor) {
    Stream<NodeContext> targetContexts;
    switch (scope) {
      case CLUSTER:
        targetContexts = cluster.nodeContexts();
        break;
      case STRIPE:
        try {
          targetContexts = cluster.getStripes().get(stripeId - 1).getNodes().stream().map(node -> new NodeContext(cluster, stripeId, node.getNodeName()));
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified stripe ID: " + stripeId + ", but cluster contains: " + cluster.getStripeCount() + " stripe(s) only");
        }
        break;
      case NODE:
        List<Node> nodes;
        try {
          nodes = cluster.getStripes().get(stripeId - 1).getNodes();
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified stripe ID: " + stripeId + ", but cluster contains: " + cluster.getStripeCount() + " stripe(s) only");
        }
        try {
          targetContexts = Stream.of(new NodeContext(cluster, stripeId, nodes.get(nodeId - 1).getNodeName()));
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified node ID: " + nodeId + ", but stripe ID: " + stripeId + " contains: " + cluster.getStripeCount() + " node(s) only");
        }
        break;
      default:
        throw new AssertionError(scope);
    }

    if (value == null) {
      targetContexts.forEach(ctx -> setting.getProperty(ctx).ifPresent(value -> setting.unsetProperty(ctx.getNode(), key)));

    } else {
      if (setting == Setting.LICENSE_FILE) {
        // do nothing, this is handled elsewhere to install a new license
        return;
      }

      if (setting == CLUSTER_NAME) {
        cluster.setName(value);
        return;
      }

      targetContexts.forEach(ctx -> {
        if (setting.requiresEagerSubstitution()) {
          setting.setProperty(ctx.getNode(), key, substitutor.substitute(value));
        } else {
          setting.setProperty(ctx.getNode(), key, value);
        }
      });
    }
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
   *  get -c stripe.1.node.1.node-backup-dir
   *  get -c stripe.1.node-backup-dir
   *  get -c node-backup-dir
   *
   *  set -c stripe.1.node.1.node-backup-dir=foo/bar
   *  set -c stripe.1.node-backup-dir=foo/bar
   *  set -c node-backup-dir=foo/bar
   * </pre>
   */
  public static Configuration valueOf(String input) {
    requireNonNull(input);
    try {
      for (Map.Entry<Pattern, BiFunction<String, Matcher, Configuration>> entry : PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(input);
        if (matcher.matches()) {
          return entry.getValue().apply(input, matcher);
        }
      }
    } catch (RuntimeException e) {
      if (e.getMessage() != null && e.getMessage().startsWith("Invalid input:")) {
        throw e;
      }
      throw new IllegalArgumentException("Invalid input: '" + input + "'. Reason: " + e.getMessage(), e);
    }
    throw new IllegalArgumentException("Invalid input: '" + input + "'.");
  }

  public static Configuration valueOf(Setting setting) {
    final String val = setting.getDefaultValue();
    return new Configuration(setting + "=" + (val == null ? "" : val), setting, CLUSTER, null, null, null, val);
  }

  public static Configuration valueOf(int stripeId, int nodeId, Setting setting) {
    final String val = setting.getDefaultValue();
    return new Configuration("stripe." + stripeId + ".node." + nodeId + "." + setting + "=" + (val == null ? "" : val), setting, NODE, stripeId, nodeId, null, val);
  }
}
