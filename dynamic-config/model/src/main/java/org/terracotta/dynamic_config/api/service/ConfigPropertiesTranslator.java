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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.terracotta.dynamic_config.api.model.Scope;
import org.terracotta.dynamic_config.api.model.Setting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.System.lineSeparator;
import static java.util.Objects.requireNonNull;

/**
 * Create a non-thread safe translator that can convert between index-based and name-based configuration settings.
 * Conversions include: (1) .cfg files containing name-based settings to a Properties object
 * comprised of the equivalent index-based settings (used during CLI import)  and (2) Properties comprised of
 * index-based settings to name-based settings, as a single string output required for .cfg files (used during CLI export).
 */

public class ConfigPropertiesTranslator {

  private static final String BLANK = "";
  private static final String DOT = ".";
  private static final String COLON = ":";
  private static final String EQUALS = "=";
  private static final String NAMES_LIST_DELIM = ",";
  private static final String NODE = "node";
  private static final String STRIPE = "stripe";
  private static final String NEW_LINE = lineSeparator();

  static final String ERR_STRIPE_NAMES_PROPERTY_MISSING = "The 'stripe-names' property was not identified.";
  static final String ERR_NO_STRIPES_IDENTIFIED = "No stripe names were identified.";
  static final String ERR_NODE_NAMES_PROPERTY_MISSING = "No 'node-names' property belonging to any stripe was identified.";
  static final String ERR_NO_NODES_IDENTIFIED = "No node names belonging to any stripe were identified.";
  static final String ERR_NO_NODES_SPECIFIED_FOR_PROPERTY = "No nodes were specified for property ";

  private final Set<String> errors = new LinkedHashSet<>();
  private final Properties converted = new Properties();
  private final Map<String, String> inputConfigSettings = new LinkedHashMap<>();
  private final Map<String, String> nodesNamespace = new TreeMap<>();
  private final Map<String, String> stripesNamespace = new TreeMap<>();
  private final StringBuilder configFileOutput = new StringBuilder();

  private enum SettingScope {
    CLUSTER,
    STRIPE,
    NODE
  }

  private enum WriteScope {
    CLUSTER_WIDE_ONLY,
    NODE_SCOPE_ONLY,
    BOTH
  }

  /**
   * Translates entries from name-based namespace syntax to index-based namespace syntax
   * and loads them into a Properties object.
   *
   * @param configFile .cfg configuration file containing name-based settings
   * @return a {@code Properties} object comprised of index-based settings
   */

  @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
  public Properties load(Path configFile) {
    requireNonNull(configFile);
    if (configFile.getFileName() == null || !configFile.getFileName().toString().endsWith(".cfg")) {
      throw new IllegalArgumentException("Expected a .cfg file, but got " + configFile.getFileName());
    }
    try (Reader in = new InputStreamReader(Files.newInputStream(configFile), StandardCharsets.UTF_8)) {
      return convert(in);
    } catch (IOException | RuntimeException e) {
      throw new IllegalArgumentException("Failed to read configuration file: " + configFile.getFileName() +
          ". Make sure the file exists, is readable and in the right format. Error: " + e.getMessage(), e);
    }
  }

  /**
   * Warning: caller who created the stream is also responsible for closing it
   */
  public Properties convert(Reader stream) throws IOException {
    // Read each line in the file stream, ignoring all line comments
    // Line format is: [[stripe|node :]<stripe_or_node_name>:]<setting>=<value>
    // splits only happen at '='
    // line continuations are supported

    errors.clear();
    inputConfigSettings.clear();
    try (BufferedReader reader = new BufferedReader(stream)) {
      List<String> lines = reader.lines()
        .filter(line -> {
           line = line.trim();
           return !line.isEmpty() && !line.startsWith("#") && !line.startsWith("!");
        })
        .collect(Collectors.toList());
      Iterator<String> itr = lines.iterator();
      String line = "";
      while (itr.hasNext()) {
        String thisLine = itr.next();
        int continuationIndex = thisLine.lastIndexOf('\\');
        if (continuationIndex == -1) {
          line += thisLine;
          String[] splits = line.split("=");
          inputConfigSettings.put(splits[0].trim(), splits.length > 1 ? splits[1].trim() : "");
          line = "";
        } else {
          line += thisLine.substring(0, continuationIndex);
        }
      }
    } catch (Exception ex) {
      throw errorsWith(ex.getMessage());
    }

    // Map node/stripe names to 'stripe.x.node.y' positions

    createStripesNamespace(); // based on ':stripe-name' entries
    createNodesNamespace();   // based on ':name' entries

    // Remove instances where a stripe name or node name property is explicitly set (we don't want to process these).
    // We add these name settings later on based on our prepared namespaces.

    inputConfigSettings.keySet().stream()
        .filter(k -> (k.endsWith(":stripe-name") || k.endsWith(":name")))
        .collect(Collectors.toList())
        .forEach(inputConfigSettings::remove);

    // At this point we have valid stripes/nodes namespace maps.
    // Validate each property (against the established stripe/node namespaces), collecting all syntax errors.

    inputConfigSettings.keySet().forEach(this::validateProperty);
    if (!errors.isEmpty()) {
      throw errors();
    }

    // Config file is error free!
    // Build the properties in the namespace index format - translating all cluster/stripe/node settings in the .cfg file.
    // Process settings in order of cluster-wide, stripe-wide then node, in order to allow node-level settings to
    // overwrite stripe/cluster-level settings and for stripe-level settings to overwrite cluster-level settings.

    converted.clear();

    // Process cluster-wide settings first
    inputConfigSettings.entrySet().stream()
        .filter(e -> isSettingAtScope(e.getKey(), SettingScope.CLUSTER))
        .forEach(ee -> addProperty(ee.getKey(), ee.getValue()));

    // Process stripe-wide settings next
    inputConfigSettings.entrySet().stream()
        .filter(e -> isSettingAtScope(e.getKey(), SettingScope.STRIPE))
        .forEach(ee -> addProperty(ee.getKey(), ee.getValue()));

    // Process all other (i.e. node-level) settings last
    inputConfigSettings.entrySet().stream()
        .filter(e -> isSettingAtScope(e.getKey(), SettingScope.NODE))
        .forEach(ee -> addProperty(ee.getKey(), ee.getValue()));

    // All props have been processed.
    // flag as error any stripe or node name that is unreferenced by any property.

    stripesNamespace.forEach((name, namespace) -> {
      if (converted.keySet().stream().noneMatch(k -> k.toString().startsWith(namespace))) {
        addError("Stripe '" + name + "' is not used (none of its nodes have any settings assigned). Consider removing it from the 'stripe-names' property.");
      }
    });
    nodesNamespace.forEach((name, namespace) -> {
      if (converted.keySet().stream().noneMatch(k -> k.toString().startsWith(namespace))) {
        addError("Node '" + name + "' has no settings assigned to it. Add at least one setting or remove it from the appropriate '<stripe>:node-names' property.");
      }
    });

    if (!errors.isEmpty()) {
      throw errors();
    }

    // No errors! Add a setting for every stripe name and node name

    stripesNamespace.forEach((name, namespace) -> converted.put(namespace + "stripe-name", name));
    nodesNamespace.forEach((name, namespace) -> converted.put(namespace + "name", name));

    return converted;
  }

  private void createStripesNamespace() {
    stripesNamespace.clear();
    //Validation
    String csvStripeNames = inputConfigSettings.get("stripe-names");
    if (csvStripeNames == null) {
      throw errorsWith(ERR_STRIPE_NAMES_PROPERTY_MISSING);
    }
    if (csvStripeNames.isEmpty()) {
      throw errorsWith(ERR_NO_STRIPES_IDENTIFIED);
    }
    List<String> stripes = split(csvStripeNames, NAMES_LIST_DELIM);
    if (stripes.isEmpty()) {
      throw errorsWith(ERR_NO_STRIPES_IDENTIFIED);
    }
    //Passed
    fillNamespace(stripesNamespace, stripes, STRIPE);
    inputConfigSettings.remove("stripe-names");
  }

  private void createNodesNamespace() {
    nodesNamespace.clear();
    List<String> nodeNamesKeys = inputConfigSettings.keySet().stream().filter(k -> k.endsWith(":node-names")).collect(Collectors.toList());
    if (nodeNamesKeys.isEmpty()) {
      throw errorsWith(ERR_NODE_NAMES_PROPERTY_MISSING);
    }
    nodeNamesKeys.forEach(k -> {
      //Validate each node-names property - collect all errors - only throw at end of method
      String stripeNamespace = validateNodeNamesProperty(k, inputConfigSettings.get(k)); //appends errors
      if (!stripeNamespace.isEmpty()) {
        fillNamespace(nodesNamespace, split(inputConfigSettings.get(k), NAMES_LIST_DELIM), stripeNamespace + NODE);
      }
      inputConfigSettings.remove(k);
    });
    if (nodesNamespace.isEmpty()) {
      addError(ERR_NO_NODES_IDENTIFIED);
    }
    if (!errors.isEmpty()) {
      throw errors();
    }
  }

  private String validateNodeNamesProperty(String nodeNamesProperty, String csvNodeNames) {

    // A bunch of validation is required.
    // If all validation passes, return the identified namespace.

    String stripeIndex = "";
    List<String> splits = split(nodeNamesProperty, COLON);
    if (splits.size() == 2) {
      // format must be <stripe_name>:node-names
      stripeIndex = validateStripeName(splits.get(0), nodeNamesProperty);
    } else if (splits.size() == 3) {
      // format must be stripe:<stripe_name>:node-names
      String scope = splits.get(0);
      if (!scope.equals(STRIPE)) {
        addError("Invalid scope for '" + nodeNamesProperty + ". Expected 'stripe' but found '" + scope + "'");
      }
      stripeIndex = validateStripeName(splits.get(1), nodeNamesProperty);
    } else {
      addError("Invalid syntax for property '" + nodeNamesProperty + "'");
    }
    if (csvNodeNames.isEmpty() || split(csvNodeNames, NAMES_LIST_DELIM).isEmpty()) {
      addError(ERR_NO_NODES_SPECIFIED_FOR_PROPERTY + nodeNamesProperty);
      stripeIndex = "";
    }
    return stripeIndex;
  }

  private String validateStripeName(String stripeName, String nodeNamesProperty) {
    String stripeIndex = "";
    if (stripeName.isEmpty()) {
      addError("Blank stripe name specified in property '" + nodeNamesProperty + "'");
    } else {
      stripeIndex = stripesNamespace.getOrDefault(stripeName, "");
      if (stripeIndex.isEmpty()) {
        addError("Stripe '" + stripeName + "' referenced in '" + nodeNamesProperty + "' not found in 'stripe-names'");
      }
    }
    return stripeIndex;
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private void validateProperty(String prop) {
    try {
      List<String> splits = split(prop, COLON);
      if (splits.size() == 3) {
        // format must be stripe|node:<stripe_or_node_name>:<setting>
        validateNamespaceForProperty(prop, splits.get(1), prop.substring(0, prop.indexOf(COLON)));
      } else if (splits.size() == 2) {
        // format must be <stripe_or_node_name>:<setting>
        validateNamespaceForProperty(prop, splits.get(0), BLANK);
      } else if (splits.size() != 1) {
        throw new IOException("Invalid syntax for property '" + prop + "'");
      }
    } catch (Exception ex) {
      addError(ex.getMessage());
    }
  }

  private void validateNamespaceForProperty(String prop, String stripeOrNodeName, String scope) throws IllegalArgumentException {
    String stripeNamespace = stripesNamespace.getOrDefault(stripeOrNodeName, "");
    String nodeNamespace = nodesNamespace.getOrDefault(stripeOrNodeName, "");
    if (scope.isEmpty()) {
      if (stripeNamespace.isEmpty() && nodeNamespace.isEmpty()) {
        throw new IllegalArgumentException("Name '" + stripeOrNodeName + "' in property '" + prop + "' is not a recognized stripe or node name");
      }
      if (!stripeNamespace.isEmpty() && !nodeNamespace.isEmpty()) {
        throw new IllegalArgumentException("Name '" + stripeOrNodeName + "' in property '" + prop + "' is both a stripe name and node name'. It must be qualified with either 'stripe:' or 'node:'");
      }
    } else if(scope.equals(STRIPE)) {
      if(stripeNamespace.isEmpty()) {
        throw new IllegalArgumentException("Stripe '" + stripeOrNodeName + "' in property '" + prop + "' is not a recognized stripe");
      }
    } else if (scope.equals(NODE)) {
      if (nodeNamespace.isEmpty()) {
        throw new IllegalArgumentException("Node '" + stripeOrNodeName + "' in property '" + prop + "' is not a recognized node");
      }
    } else {
      throw new IllegalArgumentException ("Scope '" + scope + ":' specified in property '" + prop + "' is invalid. Scope must be one of 'stripe:' or 'node:'");
    }
  }

  @SuppressFBWarnings("REC_CATCH_EXCEPTION")
  private void addProperty(String prop, String value) {
    // Note: this property, including its namespace, has already been validated with validateProperty()
    try {
      String setting = prop; // remains unchanged for cluster-wide
      String stripeOrNodeName = BLANK; // remains blank for cluster-wide
      String indexNamespace = BLANK; // remains blank for cluster-wide
      List<String> splits = split(prop, COLON);
      if (splits.size() == 3) {
        // format must be <stripe_or_node_scope>:<stripe_or_node_name>:<setting>
        stripeOrNodeName = splits.get(1);
        setting = splits.get(2);
        indexNamespace = getIndexNamespaceForStripeOrNodeName(stripeOrNodeName, prop.substring(0, prop.indexOf(COLON)));
      } else if (splits.size() == 2) {
        // format must be <stripe_or_node_name>:<setting>
        stripeOrNodeName = splits.get(0);
        setting = splits.get(1);
        indexNamespace = getIndexNamespaceForStripeOrNodeName(stripeOrNodeName, BLANK);
      }

      // if the setting is NODE-scoped but the namespace is cluster or stripe scoped then
      // add individual properties for each applicable node.

      // Get the 'root' setting (e.g. 'tc-properties' from 'tc-properties.a.b.c')
      int rootIndex = setting.indexOf(DOT);
      Setting rootSetting = Setting.fromName(setting.substring(0, rootIndex == -1 ? setting.length() : rootIndex));
      String finalSetting = setting;
      if (rootSetting.isScope(Scope.NODE) && indexNamespace.isEmpty()) {
        // create this property for all nodes in the cluster
        nodesNamespace.forEach((nn, ns) -> converted.put(ns + finalSetting, value));
      } else if (rootSetting.isScope(Scope.NODE) && stripesNamespace.containsKey(stripeOrNodeName)) {
        // create this property for all nodes in this stripe.
        String finalIndexNamespace = indexNamespace;
        nodesNamespace.values().stream()
            .filter(v -> v.startsWith(finalIndexNamespace))
            .forEach(v -> converted.put(v + finalSetting, value));
      } else {
        // this setting is one of:
        //    cluster-scoped (e.g. ssl-tls, failover-priority)
        //    stripe-scoped (e.g. stripe-name, stripe-uuid)
        //    node-scoped with a node namespace specified (e.g. stripe.1.node.2.backup-dir)
        converted.put(indexNamespace + setting, value);
      }
    } catch (Exception ex) {
      addError(ex.getMessage());
    }
  }

  private String getIndexNamespaceForStripeOrNodeName(String stripeOrNodeName, String scope) throws IllegalArgumentException  {

    String indexNamespace; // remains empty for cluster-wide
    String stripeNamespace = stripesNamespace.getOrDefault(stripeOrNodeName, "");
    String nodeNamespace = nodesNamespace.getOrDefault(stripeOrNodeName, "");
    if (scope.isEmpty()) {
      // pre-validated that stripeNamespace and nodeNamespace can not both be empty or can not both be not empty
      if (!stripeNamespace.isEmpty()) {
        indexNamespace = stripeNamespace;
      } else {
        indexNamespace = nodeNamespace;
      }
    } else if (scope.equals(STRIPE)) {
      // pre-validated that stripeNamespace is not empty
      indexNamespace = stripeNamespace;
    } else {
      // pre-validated that nodeNamespace is not empty
      indexNamespace = nodeNamespace;
    }
    return indexNamespace;
  }

  private void fillNamespace(Map<String, String> namespace, List<String> names, String scope) {
    int i = 1;
    for (String name : names) {
      if (!namespace.containsKey(name)) {
        namespace.put(name, scope + DOT + i++ + DOT);
      }
    }
  }

  private boolean isSettingAtScope(String setting, SettingScope scope)
  {
    // check if the name-based syntax setting is declared at cluster, stripe or node scope level
    boolean atScope = false;
    List<String> splits = split(setting, COLON);
    if (scope == SettingScope.CLUSTER) {
      if (splits.size() == 1) {
        atScope = true;
      }
    } else {
      String stripeOrNodeName = "";
      if (splits.size() == 3) {
        // format must be <stripe_or_node_scope>:<stripe_or_node_name>:<setting>
        stripeOrNodeName = splits.get(1);
      } else if (splits.size() == 2) {
        // format must be <stripe_or_node_name>:<setting>
        stripeOrNodeName = splits.get(0);
      }
      if (scope == SettingScope.STRIPE) {
        if (!stripesNamespace.getOrDefault(stripeOrNodeName, "").isEmpty()) {
          atScope = true;
        }
      } else if (scope == SettingScope.NODE) {
        if (!nodesNamespace.getOrDefault(stripeOrNodeName, "").isEmpty()) {
          atScope = true;
        }
      }
    }
    return atScope;
  }

  public String writeConfigOutput(Properties properties) {
    return writeConfigOutput(properties, "");
  }

  public String writeConfigOutput(Properties properties, String fileHeader) {
    return writeConfigOutput(fileHeader, properties, "", null, "", null, "");
  }

  public String writeConfigOutput(String fileHeader,
                                  Properties userDefinedProperties, String userDefinedPropertiesHeader,
                                  Properties defaultProperties, String defaultPropertiesHeader,
                                  Properties hiddenProperties, String hiddenPropertiesHeader) {

    // Extract the stripes and nodes from the user-defined properties.

    Properties userDefinedPropertiesCopy = new Properties();
    userDefinedPropertiesCopy.putAll(userDefinedProperties);

    stripesNamespace.clear();
    userDefinedProperties.entrySet().stream().filter(es -> es.getKey().toString().endsWith(".stripe-name")).forEach(e -> {
      String key = e.getKey().toString();
      stripesNamespace.put(key.substring(0, key.indexOf(".stripe-name")) + DOT, e.getValue().toString());
      userDefinedPropertiesCopy.remove(key);
    });

    nodesNamespace.clear();
    userDefinedProperties.entrySet().stream().filter(es -> es.getKey().toString().endsWith(".name")).forEach(e -> {
      String key = e.getKey().toString();
      nodesNamespace.put(key.substring(0, key.indexOf(".name")) + DOT, e.getValue().toString());
      userDefinedPropertiesCopy.remove(key);
    });

    Map<String, String> userDefinedProps = sort(userDefinedPropertiesCopy);
    Map<String, String> defaultProps = sort(defaultProperties);
    Map<String, String> hiddenProps = sort(hiddenProperties);

    // Generate the output

    configFileOutput.setLength(0);
    writeHeader(fileHeader);
    writeHeader(userDefinedPropertiesHeader);
    writeStripeNames();
    writeNodeNames();
    writeProperties(userDefinedProps, WriteScope.CLUSTER_WIDE_ONLY, false);
    writeProperties(userDefinedProps, WriteScope.NODE_SCOPE_ONLY, true);
    appendl(BLANK);

    if (!(defaultProperties == null)) {
      writeHeader(defaultPropertiesHeader);
      if (defaultProps.isEmpty()) {
        appendl("# No default properties in use.");
      } else {
        writeProperties(defaultProps, WriteScope.BOTH, false);
      }
      appendl(BLANK);
    }
    if (!(hiddenProperties == null)) {
      writeHeader(hiddenPropertiesHeader);
      if (hiddenProps.isEmpty()) {
        appendl("# No hidden properties found.");
      } else {
        writeProperties(hiddenProps, WriteScope.BOTH, false);
      }
    }
    return configFileOutput.toString();
  }

  private void writeHeader(String header)
  {
    if (!header.isEmpty()) {
      appendl("# " + header);
      appendl(BLANK);
    }
  }

  private void writeStripeNames() {
    append("stripe-names=");
    stripesNamespace.forEach((k,stripeName) -> append(stripeName + NAMES_LIST_DELIM));
    configFileOutput.delete(configFileOutput.length() - NAMES_LIST_DELIM.length(), configFileOutput.length());
    appendl(BLANK);
  }

  private void writeNodeNames() {
    stripesNamespace.forEach((k,stripeName) -> {
      append(STRIPE + COLON + stripeName + COLON + "node-names=");
      nodesNamespace.forEach((kk,nodeName) -> {
        if (kk.startsWith(k)) {
          append(nodeName + NAMES_LIST_DELIM);
        }
      });
      configFileOutput.delete(configFileOutput.length() - NAMES_LIST_DELIM.length(), configFileOutput.length());
      appendl(BLANK);
    });
    appendl(BLANK);
  }

  private void writeProperties(Map<String, String> props, WriteScope writeScope, boolean group) {

    AtomicReference<String> lastNamespace = new AtomicReference<>("");
    props.forEach((k, v) -> {
      // Check if property has exact node namespace match
      String thisNamespace = "";
      Optional<Map.Entry<String, String>> namespace = nodesNamespace.entrySet().stream().filter(es -> k.startsWith(es.getKey())).findFirst();
      if (namespace.isPresent()) {
        thisNamespace = namespace.get().getKey();
        String nodeName = namespace.get().getValue();
        String setting = k.substring(thisNamespace.length());
        if (writeScope == WriteScope.BOTH ||
            writeScope == WriteScope.NODE_SCOPE_ONLY) {
          lastNamespace.set(appendl(lastNamespace.get(), thisNamespace, group,
              NODE + COLON + nodeName + COLON + setting, v));
        }
      } else {
        // Check if the property has exact scope namespace match
        namespace = stripesNamespace.entrySet().stream().filter(es -> k.startsWith(es.getKey())).findFirst();
        if (namespace.isPresent()) {
          thisNamespace = namespace.get().getKey();
          String stripeName = namespace.get().getValue();
          String setting = k.substring(thisNamespace.length());
          if (writeScope == WriteScope.BOTH ||
              writeScope == WriteScope.NODE_SCOPE_ONLY) {
            lastNamespace.set(appendl(lastNamespace.get(), thisNamespace, group,
                STRIPE + COLON + stripeName + COLON + setting, v));
          }
        } else {
          //this is a cluster-wide setting
          if (writeScope == WriteScope.BOTH ||
              writeScope == WriteScope.CLUSTER_WIDE_ONLY) {
            lastNamespace.set(appendl(lastNamespace.get(), thisNamespace, group, k, v));
          }
        }
      }
    });
  }

  private void append(String fragment) {
    configFileOutput.append(fragment);
  }

  private void appendl(String fragment) {
    configFileOutput.append(fragment).append(NEW_LINE);
  }

  private String appendl(String lastNamespace, String thisNamespace, boolean group, String setting, String value) {
    // The 'group' flag adds a blank line whenever the namespace changes - grouping a node's properties together.
    if (group && !lastNamespace.equals(thisNamespace)) {
      lastNamespace = thisNamespace;
      appendl(BLANK);
    }
    appendl(setting + EQUALS + value);
    return lastNamespace;
  }

  private void addError(String error) {
    errors.add(error);
  }

  IllegalArgumentException errorsWith(String error) {
    errors.add(error);
    return errors();
  }

  IllegalArgumentException errors() {
    StringBuilder error = new StringBuilder();
    error.append("Error(s) were found parsing the .cfg file:").append(NEW_LINE);
    errors.forEach(s -> error.append("  ").append(s).append(NEW_LINE));
    return new IllegalArgumentException (new IOException(error.toString()));
  }

  private Map<String, String> sort(Properties props) {
    Map<String, String> sortedProps = new TreeMap<>();
    if (props != null) {
      props.forEach((k, v) -> sortedProps.put(k.toString(), v.toString()));
    }
    return sortedProps;
  }

  private List<String> split(String line, String separator) {
    return Stream.of(line.split(separator)).map(String::trim).collect(Collectors.toList());
  }
}
