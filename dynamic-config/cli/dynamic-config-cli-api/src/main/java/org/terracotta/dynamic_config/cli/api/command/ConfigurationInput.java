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
package org.terracotta.dynamic_config.cli.api.command;

import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;
import org.terracotta.dynamic_config.api.model.Stripe;
import org.terracotta.dynamic_config.api.model.UID;
import org.terracotta.dynamic_config.api.service.ClusterValidator;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;
import static org.terracotta.dynamic_config.api.model.Scope.NODE;
import static org.terracotta.dynamic_config.api.model.Scope.STRIPE;

public class ConfigurationInput {

  private static final Map<Pattern, BiConsumer<ConfigurationInput, Matcher>> CFG_STRIPE_PATTERNS = new LinkedHashMap<>();
  private static final Map<Pattern, BiConsumer<ConfigurationInput, Matcher>> CFG_NODE_PATTERNS = new LinkedHashMap<>();
  private static final Map<Pattern, BiConsumer<ConfigurationInput, Matcher>> CFG_STRIPE_OR_NODE_PATTERNS = new LinkedHashMap<>();

  private static final String CFG_STRIPE_SEP = "stripe:";
  private static final String CFG_NODE_SEP = "node:";
  // match all chars for stripe/node names except 'forbidden' chars (see FORBIDDEN_FILE_CHARS and FORBIDDEN_DC_CHARS in ClusterValidator)
  private static final String CFG_GRP_NAME = "([^:/\\\\<>\"|*? ,=%{}]+):";
  private static final String SEP = "\\.";
  private static final String GRP_SETTING = "([a-z\\-]+)";
  private static final String GRP_KEY = "([^=:]+)";
  private static final String ASSIGN = "=";
  private static final String GRP_VALUE = "([^=]+)";

  static {
    // stripe:<name>:<setting>.<key>
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        matcher.group(3),
        null));
    // stripe:<name>:<setting>
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        null,
        null));
    // stripe:<name>:<setting>.<key>=
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        matcher.group(3),
        ""));
    // stripe:<name>:<setting>=
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        null,
        ""));
    // stripe:<name>:<setting>.<key>=<value>
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        matcher.group(3),
        matcher.group(4)));
    // stripe:<name>:<setting>=<value>
    CFG_STRIPE_PATTERNS.put(Pattern.compile("^" + CFG_STRIPE_SEP + CFG_GRP_NAME + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        STRIPE,
        matcher.group(1),
        null,
        matcher.group(3)));
    // node:<name>:<setting>.<key>=<value>
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        matcher.group(3),
        matcher.group(4)));
    // node:<name>:<setting>=<value>
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        null,
        matcher.group(3)));
    // node:<name>:<setting>.<key>=
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        matcher.group(3),
        ""));
    // node:<name>:<setting>=
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        null,
        ""));
    // node:<name>:<setting>.<key>
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        matcher.group(3),
        null));
    // node:<name>:<setting>
    CFG_NODE_PATTERNS.put(Pattern.compile("^" + CFG_NODE_SEP + CFG_GRP_NAME + GRP_SETTING + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        NODE,
        matcher.group(1),
        null,
        null));
    // <name>:<setting>.<key>=<value>
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        matcher.group(3),
        matcher.group(4)));
    // <name>:<setting>=<value>
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + ASSIGN + GRP_VALUE + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        null,
        matcher.group(3)));
    // <name>:<setting>.<key>=
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        matcher.group(3),
        ""));
    // <name>:<setting>=
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + ASSIGN + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        null,
        ""));
    // <name>:<setting>.<key>
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + SEP + GRP_KEY + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        matcher.group(3),
        null));
    // <name>:<setting>
    CFG_STRIPE_OR_NODE_PATTERNS.put(Pattern.compile("^" + CFG_GRP_NAME + GRP_SETTING + "$"), (input, matcher) -> input.update(
        Setting.fromName(matcher.group(2)),
        null,
        matcher.group(1),
        null,
        null));
  }

  private final String cliInput;
  private String stripeOrNodeName= null;
  private Setting setting= null;
  private Scope level= null;
  private String key= null;
  private String value= null;

  /***
   * Every CLI get, set and unset -setting is instantiated through here.
   * If the setting is based on the name-based syntax then this class handles all functionality.
   * If the setting is based on the index-based syntax then the Configuration class is
   * called upon (to satisfy subsequent requests).
   */
  public ConfigurationInput(String cliInput) {
    requireNonNull(cliInput);
    this.cliInput = cliInput.trim();
    if (parseAsNamedProperty()) {
      // the remainder of this class's members have been updated at this point
      validate();
    }
  }

  /***
   * If this CLI -setting instance uses the name-based syntax, then these properties are
   * updated based on the static matcher patterns.
   */
  private void update(Setting setting, Scope level, String name, String key, String value) {
    this.setting = requireNonNull(setting);
    this.stripeOrNodeName = requireNonNull(name);
    this.level = level; // can be null (user may not have specified 'stripe:' or 'node:' qualifier)
    this.key = key;
    this.value = value == null ? null : value.trim();
  }

  private Boolean parseAsNamedProperty() {

    // Scan the input and determine if it is a name-based namespace property.
    // If it is and there are fundamental syntax errors, then throw.  If it's determined the configuration
    // is not named-based then return false and let Configuration() process it.

    // Name-based namespace properties are of the form:
    // [[stripe|node :]<stripe_or_node_name>:]<setting>=<value>

    String scope = "";
    String setting;
    String[] s = cliInput.split("=")[0].split(":");
    if (s.length == 3) {
      // This is most likely a named-based formatted setting
      // Therefore, format must be: stripe|node:<stripe_or_node_name>:<setting>.
      // All we can validate at this point is the 'scope' identifier (we can't validate the stripe/node name yet)
      scope = s[0];
      if (!scope.equals("stripe") && !scope.equals("node")) {
        throw new IllegalArgumentException ("Scope '" + scope + ":' specified in property '" + cliInput + "' is invalid. Scope must be one of 'stripe:' or 'node:'");
      }
      // assume s[1] is a valid stripe or node name - can only validate later on
      setting = s[2];
    } else if (s.length == 2) {
      // format MUST be <stripe_or_node_name>:<setting>
      // assume s[0] is a valid stripe or node name - can only validate later on
      setting = s[1];
    } else {
      // Could be a cluster-wide setting or an index-based setting or a mis-configured setting.
      return false;
    }
    //throw for unrecognized setting name
    int idx = setting.indexOf(".");
    Setting.fromName(setting.substring(0, idx == -1 ? setting.length() : idx));

    if (scope.equals("node")) {
      for (Map.Entry<Pattern, BiConsumer<ConfigurationInput, Matcher>> entry : CFG_NODE_PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(cliInput);
        if (matcher.matches()) {
          entry.getValue().accept(this, matcher); // calls update()
          return true;
        }
      }
    }
    if (scope.equals("stripe")) {
      for (Map.Entry<Pattern, BiConsumer<ConfigurationInput, Matcher>> entry : CFG_STRIPE_PATTERNS.entrySet()) {
        Matcher matcher = entry.getKey().matcher(cliInput);
        if (matcher.matches()) {
          entry.getValue().accept(this, matcher); // calls update()
          return true;
        }
      }
    }
    // no scope has been defined (can be stripe or node)
    for (Map.Entry<Pattern, BiConsumer<ConfigurationInput, Matcher>> entry : CFG_STRIPE_OR_NODE_PATTERNS.entrySet()) {
      Matcher matcher = entry.getKey().matcher(cliInput);
      if (matcher.matches()) {
        entry.getValue().accept(this, matcher); // calls update()
        return true;
      }
    }
    throw new IllegalArgumentException("Invalid input: '" + cliInput + "'");
  }

  private void validate() {
    String scope;
    if (level == STRIPE) {
      scope = "stripe";
    } else if (level == NODE) {
      scope = "node";
    } else {
      scope = "stripe or node";
    }
    ClusterValidator.validateName(stripeOrNodeName, scope);

    if (!setting.isMap() && key != null) {
      throw new IllegalArgumentException("Invalid input: '" + cliInput + "'. Reason: Setting '" + setting + "' is not a map and must not have a key");
    }
    if (level != null) {
      if (!setting.allows(level)) {
        throw new IllegalArgumentException("Invalid input: '" + cliInput + "'. Reason: Setting '" + setting + "' does not allow any operation at " + level + " level");
      }
      try {
        setting.validate(key, value, level);
      } catch (RuntimeException e) {
        throw new IllegalArgumentException("Invalid input: '" + cliInput + "'. Reason: " + e.getMessage(), e);
      }
    }
  }

  /***
   * If this ConfigurationInput is derived from a name-based syntax setting, then deduce the correct
   * stripeId, nodeId and/or scope values based on the supplied cluster and return a Configuration
   * instance derived from that.  Else, pass the work off to Configuration.valueIf() directly.
   */
  public Configuration toConfiguration(Cluster cluster) {
    Configuration config = valueOf(cluster);
    if (config == null){
      config = Configuration.valueOf(cliInput);
    }
    return config;
  }

  public boolean isNamedProperty() {
    // The 'hook' that determines if this ConfigurationInput instance was entered at the command line
    // using the name-based syntax, is if the 'stripeOrNodeName' property has been set.
    // If no stripe or node name has been specified, then this is an index-based syntax setting and
    // the Configuration class handles things.

    return stripeOrNodeName != null && !stripeOrNodeName.isEmpty();
  }

  private Configuration valueOf(Cluster cluster) {

    if (!isNamedProperty()) {
      // This ConfigurationInput instance was created from an index-based namespace cli input.
      // The Configuration class handles that.
      return null;
    }

    Optional<Node> node = cluster.getNodeByName(stripeOrNodeName);
    Optional<Stripe> stripe = cluster.getStripeByName(stripeOrNodeName);

    if (level == null) {
      // user did not specify the scope - only <node_or_stripe_name>:<setting>
      if (node.isPresent() && stripe.isPresent()) {
        throw new IllegalArgumentException(format("Name '%s' in setting '%s' is both a stripe name and node name. " +
            "It must be qualified with either 'stripe:' or 'node:'", stripeOrNodeName, cliInput));
      }
    } else {
      if (level.equals(Scope.STRIPE) && !stripe.isPresent()) {
        throw new IllegalArgumentException(format("Name '%s' in setting '%s' is not a recognized stripe.", stripeOrNodeName, cliInput));
      }
      if (level.equals(Scope.NODE) && !node.isPresent()) {
        throw new IllegalArgumentException(format("Name '%s' in setting '%s' is not a recognized node.", stripeOrNodeName, cliInput));
      }
    }
    if (!stripe.isPresent() && !node.isPresent()) {
      throw new IllegalArgumentException(format("Name '%s' in setting '%s' is not a recognized stripe or node.", stripeOrNodeName, cliInput));
    }

    // Update the stripeId and/or nodeId based on the scope level

    if (level == null) {
      level = stripe.isPresent() ? Scope.STRIPE : Scope.NODE;
    }
    int stripeId;
    Integer nodeId = null;
    if (level.equals(Scope.STRIPE)) {
      stripeId = cluster.getStripeId(stripe.get().getUID()).getAsInt();
    } else {
      UID nodeUid = node.get().getUID();
      stripeId = cluster.getStripeIdByNode(nodeUid).getAsInt();
      nodeId = cluster.getNodeId(nodeUid).getAsInt();
    }
    return new Configuration(cliInput, setting, level, stripeId, nodeId, key, value);
  }

  /***
   * Extract the Setting embedded in the CLI input. First try parsing it as a 'named-based'
   * syntax and then as an 'index-based' syntax (if needed).
   * @param cliInput setting value entered at the command line
   */
  public static Setting getSetting(String cliInput) {
    ConfigurationInput c = new ConfigurationInput(cliInput);
    if (c.isNamedProperty()) {
      return c.setting;
    } else {
      return Configuration.valueOf(c.cliInput).getSetting();
    }
  }
  /***
   * Extract the Key (it if exists) for the CLI input. First try parsing it as a 'named-based'
   * syntax and then as an 'index-based' syntax (if needed).
   * @param cliInput setting value entered at the command line
   */
  public static String getKey(String cliInput) {
    ConfigurationInput c = new ConfigurationInput(cliInput);
    if (c.isNamedProperty()) {
      return c.key;
    } else {
      return Configuration.valueOf(c.cliInput).getKey();
    }
  }
}
