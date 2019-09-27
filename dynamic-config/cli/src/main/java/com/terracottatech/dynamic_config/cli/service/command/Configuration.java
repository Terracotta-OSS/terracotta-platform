/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Operation;
import com.terracottatech.dynamic_config.model.Scope;
import com.terracottatech.dynamic_config.model.Setting;
import com.terracottatech.dynamic_config.nomad.Applicability;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;

import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.terracottatech.dynamic_config.model.Operation.GET;
import static com.terracottatech.dynamic_config.model.Operation.SET;
import static com.terracottatech.dynamic_config.model.Operation.UNSET;
import static com.terracottatech.dynamic_config.model.Scope.CLUSTER;
import static com.terracottatech.dynamic_config.model.Scope.NODE;
import static com.terracottatech.dynamic_config.model.Scope.STRIPE;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

public class Configuration {

  private static final IParameterSubstitutor SUBSTITUTOR = IParameterSubstitutor.unsupported();
  private static final Map<Pattern, BiFunction<String, Matcher, Configuration>> PATTERNS = new IdentityHashMap<>();

  private static final String GRP_STRIPE = "stripe\\.(\\d+)";
  private static final String GRP_NODE = "node\\.(\\d+)";
  private static final String SEP = "\\.";
  private static final String NS = "[.:]"; // namespace separator
  private static final String GRP_SETTING = "([a-z-]+)";
  private static final String GRP_KEY = "([^=:.]+)";
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
  }

  public Scope getScope() {
    return scope;
  }

  public OptionalInt getStripeId() {
    return stripeId == null ? OptionalInt.empty() : OptionalInt.of(stripeId);
  }

  public OptionalInt getNodeId() {
    return nodeId == null ? OptionalInt.empty() : OptionalInt.of(nodeId);
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

  public void validate(Operation operation) {
    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " is not a map and must not have a key name");
    }
    if (stripeId != null && stripeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected stripe id to be greater than 0");
    }
    if (nodeId != null && nodeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected node id to be greater than 0");
    }
    if (value != null && !SUBSTITUTOR.containsSubstitutionParams(value)) {
      try {
        setting.validate(key, value);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + e.getMessage(), e);
      }
    }
    if (!setting.allowsOperation(operation)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " does not allow operation " + operation);
    }
    if (value == null && operation.isValueRequired()) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " requires a value");
    }
    if (value != null && !operation.isValueRequired()) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " must not have a value");
    }
    if (operation == SET || operation == UNSET) {
      // only validate the scope where to apply the change in case of a set operation. Get can use any scope.
      if (!setting.allowsScope(scope)) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Setting " + setting + " does not allow scope " + scope);
      }
    }
  }

  public boolean matchConfigPropertyKey(String key) {
    Configuration config = Configuration.valueOf(key);
    if (config.scope != NODE || config.nodeId == null || config.stripeId == null) {
      throw new IllegalArgumentException(key + " is not a valid property key from a property configuration");
    }
    if (config.setting != this.setting) {
      return false;
    }
    if (this.stripeId != null && !Objects.equals(this.stripeId, config.stripeId)) {
      // we want to match a node or stripe scope
      return false;
    }
    if (this.nodeId != null && !Objects.equals(this.nodeId, config.nodeId)) {
      // we want to match a specific node
      return false;
    }
    if (this.setting.isMap() && this.key != null && !Objects.equals(this.key, config.key)) {
      // case of a map, where we want to match a specific key
      return false;
    }
    if (this.setting.isMap() && this.key == null && config.key != null) {
      // case of a map, where we want to match a specific key
      return false;
    }
    return true;
  }

  public void apply(Operation operation, Cluster cluster) {
    if (operation == GET) {
      throw new IllegalArgumentException("Command " + GET + " is not a mutation operation");
    }

    Collection<Node> target;
    switch (scope) {
      case CLUSTER:
        target = cluster.getNodes();
        break;
      case STRIPE:
        try {
          target = cluster.getStripes().get(stripeId - 1).getNodes();
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified stripe id: " + stripeId + ", but cluster contains: " + cluster.getStripes().size() + " stripe(s) only");
        }
        break;
      case NODE:
        List<Node> nodes = null;
        try {
          nodes = cluster.getStripes().get(stripeId - 1).getNodes();
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified stripe id: " + stripeId + ", but cluster contains: " + cluster.getStripes().size() + " stripe(s) only");
        }
        try {
          target = singletonList(nodes.get(nodeId - 1));
        } catch (Exception e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Specified node id: " + nodeId + ", but stripe " + stripeId + " contains: " + cluster.getStripes().size() + " node(s) only");
        }
        break;
      default:
        throw new AssertionError(scope);
    }

    if (operation == UNSET) {
      target.forEach(node -> setting.unsetProperty(node, key));
      return;
    }

    if (operation == SET) {
      if (setting == Setting.LICENSE_FILE) {
        // do nothing, this is handled elsewhere to install a new license
        return;
      }

      if (setting == Setting.CLUSTER_NAME) {
        cluster.setName(value);
        return;
      }

      target.forEach(node -> setting.setProperty(node, key, value));
    }
  }

  public SettingNomadChange toSettingNomadChange(Operation operation, Cluster cluster) {
    validate(operation);
    switch (operation) {
      case SET:
        return SettingNomadChange.set(toApplicability(cluster), setting, key, value);
      case UNSET:
        return SettingNomadChange.unset(toApplicability(cluster), setting, key);
      default:
        throw new IllegalArgumentException("Operation " + operation + " cannot be converted to a Nomad change for an active cluster");
    }
  }

  private Applicability toApplicability(Cluster cluster) {
    switch (scope) {
      case NODE:
        return Applicability.node(stripeId, cluster.getNode(stripeId, nodeId).getNodeName());
      case STRIPE:
        return Applicability.stripe(stripeId);
      case CLUSTER:
        return Applicability.cluster();
      default:
        throw new AssertionError(scope);
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
      throw new IllegalArgumentException("Invalid input: '" + input + "'. Reason: " + e.getMessage(), e);
    }
    throw new IllegalArgumentException("Invalid input: '" + input + "'. Config property should be one of the format:\n" +
        " - stripe.<index>.node.<index>:<setting>.<key>=<value>\n" +
        " - stripe.<index>.node.<index>:<setting>=<value>\n" +
        " - stripe.<index>:<setting>.<key>=<value>\n" +
        " - stripe.<index>:<setting>=<value>\n" +
        " - <setting>.<key>=<value>\n" +
        " - <setting>=<value>\n" +
        " - stripe.<index>.node.<index>:<setting>.<key>\n" +
        " - stripe.<index>.node.<index>:<setting>\n" +
        " - stripe.<index>:<setting>.<key>\n" +
        " - stripe.<index>:<setting>\n" +
        " - <setting>.<key>\n" +
        " - <setting>"
    );
  }
}
