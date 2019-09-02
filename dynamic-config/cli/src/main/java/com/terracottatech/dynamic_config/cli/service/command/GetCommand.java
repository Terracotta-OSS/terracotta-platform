/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service.command;

import com.beust.jcommander.Parameters;
import com.terracottatech.diagnostic.client.connection.MultiDiagnosticServiceConnection;
import com.terracottatech.dynamic_config.cli.common.Usage;
import com.terracottatech.dynamic_config.diagnostic.TopologyService;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.config.CommonOptions;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;

import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.terracottatech.dynamic_config.model.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_WHITELIST;

@Parameters(commandNames = "get", commandDescription = "Display properties of nodes")
@Usage("get -s HOST -c PROPERTY1,PROPERTY2,...")
public class GetCommand extends PropertyCommand {
  private final Map<String, String> outputMap = new LinkedHashMap<>();

  @Override
  public void validate() {
    for (String configString : configs) {
      String[] scopeAndProperty = configString.split(":");
      switch (scopeAndProperty.length) {
        case 1:
          //CLUSTER scope
          parse(configString, scopeAndProperty[0], Scope.CLUSTER, -1, -1);
          break;
        case 2:
          //STRIPE or NODE scope
          String scope = scopeAndProperty[0];
          String[] scopeIdentifiers = scope.split("\\.");
          int stripeId;

          switch (scopeIdentifiers.length) {
            case 4:
              if (!scopeIdentifiers[0].equals("stripe") || !scopeIdentifiers[2].equals("node")) {
                throwException("Config property is of the format: stripe.<index>.node.<index>:<property>.<property-name>");
              }
              stripeId = validateIndex(scopeIdentifiers[1], "Expected stripe id to be greater than 0");
              int nodeId = validateIndex(scopeIdentifiers[3], "Expected node id to be greater than 0");
              parse(configString, scopeAndProperty[1], Scope.NODE, stripeId, nodeId);
              break;
            case 2:
              if (!scopeIdentifiers[0].equals("stripe")) {
                throwException("Config property is of the format: stripe.<index>:<property>.<property-name>");
              }
              stripeId = validateIndex(scopeIdentifiers[1], "Expected stripe id to be greater than 0");
              parse(configString, scopeAndProperty[1], Scope.STRIPE, stripeId, -1);
              break;
            default:
              throwException("Expected scope to be either in stripe.<index>.node.<index>, or stripe.<index> format, but found: %s", scope);
          }
          break;
        default:
          throwException("Expected 0 or 1 scope-resolution colon characters, but got: %s in input: %s", scopeAndProperty.length, configString);
      }
    }
    logger.debug("Command validation successful");
  }

  @Override
  public void run() {
    try (MultiDiagnosticServiceConnection singleConnection = connectionFactory.createConnection(node)) {
      Cluster cluster = getTopologyService(singleConnection).getCluster();
      List<Stripe> stripes = cluster.getStripes();
      for (ParsedInput input : parsedInput) {
        if (input.getScope() == Scope.CLUSTER) {
          for (int i = 0; i < stripes.size(); i++) {
            Stripe stripe = stripes.get(i);
            for (int j = 0; j < stripe.getNodes().size(); j++) {
              String prefix = "stripe." + (i + 1) + ".node." + (j + 1) + ".";
              getNodeProperty(input, stripe.getNodes().get(j), prefix + input.getRawInput());
            }
          }
        } else {
          if (stripes.size() < input.getStripeId()) {
            throwException("Specified stripe id: %s, but cluster contains: %s stripe(s) only", input.getStripeId(), stripes.size());
          }
          List<Node> nodes = stripes.get(input.getStripeId() - 1).getNodes();

          if (input.getScope() == Scope.STRIPE) {
            for (int i = 0; i < nodes.size(); i++) {
              String processedInput = input.getRawInput().replace(":", ".node." + (i + 1) + ".");
              getNodeProperty(input, nodes.get(i), processedInput);
            }
          } else {
            if (nodes.size() < input.getNodeId()) {
              throwException("Specified node id: %s, but stripe %s contains: %s node(s) only", input.getNodeId(), input.getStripeId(), nodes.size());
            }
            Node targetNode = nodes.get(input.getNodeId() - 1);
            String processedInput = input.getRawInput().replace(":", ".");
            getNodeProperty(input, targetNode, processedInput);
          }
        }
      }
    }

    logger.info(formatOutput() + "\n");
  }

  private String formatOutput() {
    return outputMap.entrySet()
        .stream()
        .map(entry -> entry.getKey() + "=" + entry.getValue())
        .collect(Collectors.joining("\n"));
  }

  private TopologyService getTopologyService(MultiDiagnosticServiceConnection singleConnection) {
    return singleConnection.getDiagnosticService(node).get().getProxy(TopologyService.class);
  }

  private void parse(String configString, String propertyString, Scope cluster, int stripeId, int nodeId) {
    String[] propertyAndName = propertyString.split("\\.");

    if (propertyAndName.length != 1 && propertyAndName.length != 2) {
      throwException("Expected property in the format <property>.<property-name>, but found: %s in input: %s", propertyString, configString);
    } else {
      Collection<String> allOptions = CommonOptions.getAllOptions();
      if (!allOptions.contains(propertyAndName[0])) {
        throwException("Unknown property: %s in input: %s. Valid properties are: %s", propertyAndName[0], configString, allOptions);
      }

      if (propertyAndName.length == 1) {
        parsedInput.add(new ParsedInput(configString, cluster, stripeId, nodeId, propertyAndName[0], null, null));
      } else {
        parsedInput.add(new ParsedInput(configString, cluster, stripeId, nodeId, propertyAndName[0], propertyAndName[1], null));
      }
    }
  }

  private void getNodeProperty(ParsedInput input, Node targetNode, String key) {
    switch (input.getProperty()) {
      case OFFHEAP_RESOURCES:
        Map<String, Measure<MemoryUnit>> offheapResources = targetNode.getOffheapResources();
        if (input.getPropertyName() != null) {
          Measure<MemoryUnit> found = offheapResources.get(input.getPropertyName());
          if (found == null) {
            throwException("No offheap-resource match found for: %s. Available offheap-resources are: %s", input.getPropertyName(), offheapResources.keySet());
          }
          outputMap.put(key, found.toString());
        } else {
          String allOffheaps = offheapResources.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().toString()).collect(Collectors.joining(","));
          outputMap.put(key, allOffheaps);
        }
        break;
      case DATA_DIRS:
        Map<String, Path> dataDirs = targetNode.getDataDirs();
        if (input.getPropertyName() != null) {
          Path found = dataDirs.get(input.getPropertyName());
          if (found == null) {
            throwException("No data-dir match found for: %s. Available data-dirs are: %s", input.getPropertyName(), dataDirs.keySet());
          }
          outputMap.put(key, found.toString());
        } else {
          String allDataDirs = dataDirs.entrySet().stream().map(entry -> entry.getKey() + ":" + entry.getValue().toString()).collect(Collectors.joining(","));
          outputMap.put(key, allDataDirs);
        }
        break;
      case SECURITY_SSL_TLS:
      case SECURITY_WHITELIST:
        invokeGetter(input, targetNode, key, "is");
        break;
      default:
        invokeGetter(input, targetNode, key, "get");
    }
  }

  private void invokeGetter(ParsedInput input, Node targetNode, String key, String methodPrefix) {
    Matcher matcher = Pattern.compile("-([a-z])").matcher(input.getProperty());
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
    }
    matcher.appendTail(sb);
    String methodName = methodPrefix + sb.replace(0, 1, String.valueOf(sb.charAt(0)).toUpperCase()).toString();
    try {
      Object targetProperty = Node.class.getDeclaredMethod(methodName).invoke(targetNode);
      outputMap.put(key, targetProperty == null ? "" : targetProperty.toString());
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new AssertionError("Could not invoke any getter for property: " + input.getProperty());
    }
  }
}
