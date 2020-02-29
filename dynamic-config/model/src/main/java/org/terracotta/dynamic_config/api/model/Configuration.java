/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model;

import org.terracotta.dynamic_config.api.service.IParameterSubstitutor;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.terracotta.dynamic_config.api.model.Operation.CONFIG;
import static org.terracotta.dynamic_config.api.model.Operation.GET;
import static org.terracotta.dynamic_config.api.model.Operation.SET;
import static org.terracotta.dynamic_config.api.model.Operation.UNSET;
import static org.terracotta.dynamic_config.api.model.Scope.CLUSTER;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;
import static org.terracotta.dynamic_config.api.model.Setting.CLUSTER_NAME;

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
    this.value = value == null || value.trim().isEmpty() ? null : value.trim();

    // pre-validate with the real value taken from input
    preValidate(value);
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

  private void preValidate(String rawValue) {
    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " is not a map and must not have a key");
    }
    if (stripeId != null && stripeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected stripe ID to be greater than 0");
    }
    if (nodeId != null && nodeId <= 0) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Expected node ID to be greater than 0");
    }
    if (!setting.allowsAnyOperationInScope(scope)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " does not allow any operation at " + scope + " level");
    }
    if (rawValue == null) {
      // equivalent to a get or unset command - we do not know yet, so we cannot pre-validate
      // byt if the setting is not supporting both get and unset, then fail
      if (!setting.allowsOperation(GET) && !setting.allowsOperation(UNSET)) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " cannot be read or cleared");
      }
    } else if (rawValue.isEmpty()) {
      // equivalent to an unset or config because no value after equal sign
      // - cluster-name= (in config file)
      // - unset node-backup-dir=
      // - set node-backup-dir=
      if (setting.isRequired()) {
        // unset is not supported at all
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " requires a value");
      } else if (!setting.allowsOperationInScope(UNSET, scope) && !setting.allowsOperationInScope(CONFIG, scope)) {
        // unset is not supported in the given scope
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " cannot be cleared at " + scope + " level");
      }
    } else {
      // equivalent to a set because we have a value
      if (!setting.allowsOperation(SET) && !setting.allowsOperation(CONFIG)) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " cannot be set");
      } else if (!setting.allowsOperationInScope(SET, scope) && !setting.allowsOperationInScope(CONFIG, scope)) {
        throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " cannot be set at " + scope + " level");
      }
      // check the value if we have one
      if (!IParameterSubstitutor.containsSubstitutionParams(value)) {
        try {
          setting.validate(key, value);
        } catch (RuntimeException e) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + e.getMessage(), e);
        }
      }
    }
  }

  public void validate(Operation operation) {
    if (!setting.allowsOperationInScope(operation, scope)) {
      throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: " + setting + " does not allow operation " + operation + " at " + scope + " level");
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
        if (value == null && setting.isRequired()) {
          throw new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Operation " + operation + " requires a value");
        }
        break;
      default:
        throw new AssertionError(operation);
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
    if (scope != other.scope) {
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

  /**
   * Apply the value in this configuration in the given cluster
   */
  public void apply(Cluster cluster) {
    Stream<NodeContext> targetContexts;
    switch (scope) {
      case CLUSTER:
        targetContexts = cluster.nodeContexts();
        break;
      case STRIPE:
        targetContexts = cluster.getStripe(stripeId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid stripe ID: " + stripeId + ". Cluster contains: " + cluster.getStripeCount() + " stripe(s)"))
            .getNodes().stream().map(node -> new NodeContext(cluster, stripeId, node.getNodeName()));
        break;
      case NODE:
        targetContexts = Stream.of(new NodeContext(cluster, stripeId, cluster.getStripe(stripeId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid stripe ID: " + stripeId + ". Cluster contains: " + cluster.getStripeCount() + " stripe(s)"))
            .getNode(nodeId)
            .orElseThrow(() -> new IllegalArgumentException("Invalid input: '" + rawInput + "'. Reason: Invalid node ID: " + nodeId + ". Stripe ID: " + stripeId + " contains: " + cluster.getStripe(stripeId).get().getNodeCount() + " node(s)"))
            .getNodeName()));
        break;
      default:
        throw new AssertionError(scope);
    }

    if (value == null) {
      targetContexts.forEach(ctx -> setting.getProperty(ctx).ifPresent(value -> setting.setProperty(ctx.getNode(), key, null)));

    } else {
      if (setting == Setting.LICENSE_FILE) {
        // do nothing, this is handled elsewhere to install a new license
        return;
      }

      if (setting == CLUSTER_NAME) {
        cluster.setName(value);
        return;
      }

      targetContexts.forEach(ctx -> setting.setProperty(ctx.getNode(), key, value));
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

  public static Configuration valueOf(Setting setting) {
    String val = setting.getDefaultValue();
    if (val == null) {
      // simulate what we would have in a config file, such as: node-backup-dir=
      val = "";
    }
    return new Configuration(setting + "=" + val, setting, CLUSTER, null, null, null, val);
  }

  public static Configuration valueOf(Setting setting, int stripeId) {
    String val = setting.getDefaultValue();
    if (val == null) {
      // simulate what we would have in a config file, such as: node-backup-dir=
      val = "";
    }
    return new Configuration("stripe." + stripeId + "." + setting + "=" + val, setting, STRIPE, stripeId, null, null, val);
  }

  public static Configuration valueOf(Setting setting, int stripeId, int nodeId) {
    String val = setting.getDefaultValue();
    if (val == null) {
      // simulate what we would have in a config file, such as: node-backup-dir=
      val = "";
    }
    return new Configuration("stripe." + stripeId + ".node." + nodeId + "." + setting + "=" + val, setting, NODE, stripeId, nodeId, null, val);
  }
}
