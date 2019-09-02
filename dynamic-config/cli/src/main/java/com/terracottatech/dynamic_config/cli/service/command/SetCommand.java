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
import com.terracottatech.dynamic_config.model.validation.NodeParamsValidator;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.TimeUnit;
import com.terracottatech.utilities.Tuple2;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.terracottatech.dynamic_config.DynamicConfigConstants.PARAM_INTERNAL_SEP;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_LEASE_DURATION;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.CLIENT_RECONNECT_WINDOW;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.DATA_DIRS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_BACKUP_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_GROUP_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_PORT;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.NODE_REPOSITORY_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.OFFHEAP_RESOURCES;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_AUDIT_LOG_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_DIR;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_SSL_TLS;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.SECURITY_WHITELIST;
import static com.terracottatech.dynamic_config.model.config.CommonOptions.TC_PROPERTIES;

@Parameters(commandNames = "set", commandDescription = "Set properties in the cluster")
@Usage("set -s HOST -c PROPERTY1=VALUE1,PROPERTY2=VALUE2,...")
public class SetCommand extends PropertyCommand {
  @Override
  public void validate() {
    for (String configString : configs) {
      String[] keyValue = configString.split("=");
      if (keyValue.length != 2) {
        throwException("Config property is of the format: <scope>:<property>=<value>");
      }

      String[] scopeAndProperty = keyValue[0].split(":");
      switch (scopeAndProperty.length) {
        case 1:
          //CLUSTER scope
          parse(configString, scopeAndProperty[0], Scope.CLUSTER, -1, -1, keyValue[1]);
          break;
        case 2:
          //STRIPE or NODE scope
          String scope = scopeAndProperty[0];
          String[] scopeIdentifiers = scope.split("\\.");
          int stripeId;

          switch (scopeIdentifiers.length) {
            case 4:
              if (!scopeIdentifiers[0].equals("stripe") || !scopeIdentifiers[2].equals("node")) {
                throwException("Config property is of the format: stripe.<index>.node.<index>:<property>.<property-name>=<value>");
              }
              stripeId = validateIndex(scopeIdentifiers[1], "Expected stripe id to be greater than 0");
              int nodeId = validateIndex(scopeIdentifiers[3], "Expected node id to be greater than 0");
              parse(configString, scopeAndProperty[1], Scope.NODE, stripeId, nodeId, keyValue[1]);
              break;
            case 2:
              if (!scopeIdentifiers[0].equals("stripe")) {
                throwException("Config property is of the format: stripe.<index>:<property>.<property-name>");
              }
              stripeId = validateIndex(scopeIdentifiers[1], "Expected stripe id to be greater than 0");
              parse(configString, scopeAndProperty[1], Scope.STRIPE, stripeId, -1, keyValue[1]);
              break;
            default:
              throwException("Expected scope to be either in stripe.<index>.node.<index>, or stripe.<index> format, but found: %s", scope);
          }
          break;
        default:
          throwException("Expected 0 or 1 scope-resolution colon characters, but got: %s in input: %s", scopeAndProperty.length, configString);
      }
    }
    validateNodeParams();
    logger.debug("Command validation successful");
  }

  @Override
  public void run() {
    try (MultiDiagnosticServiceConnection singleConnection = connectionFactory.createConnection(node)) {
      Cluster cluster = getTopologyService(singleConnection).getCluster();
      if (getTopologyService(singleConnection).isActivated()) {
        throw new UnsupportedOperationException("TODO [DYNAMIC-CONFIG]: To be implemented in TDB-4654");
      }

      List<Stripe> stripes = cluster.getStripes();
      for (ParsedInput input : parsedInput) {
        if (input.getScope() == Scope.CLUSTER) {
          if (!getTopologyService(singleConnection).isActivated()) {
            cluster.getNodes().forEach(node1 -> setNodeProperty(input, node1));
          }
        } else {
          if (stripes.size() < input.getStripeId()) {
            throwException("Specified stripe id: %s, but cluster contains: %s stripe(s) only", input.getStripeId(), stripes.size());
          }
          List<Node> nodes = stripes.get(input.getStripeId() - 1).getNodes();

          if (input.getScope() == Scope.STRIPE) {
            if (!getTopologyService(singleConnection).isActivated()) {
              stripes.get(input.getStripeId() - 1)
                  .getNodes()
                  .forEach(node1 -> setNodeProperty(input, node1));
            }
          } else {
            if (nodes.size() < input.getNodeId()) {
              throwException("Specified node id: %s, but stripe %s contains: %s node(s) only", input.getNodeId(), input.getStripeId(), nodes.size());
            }
            Node targetNode = nodes.get(input.getNodeId() - 1);
            if (!getTopologyService(singleConnection).isActivated()) {
              stripes.get(input.getStripeId() - 1)
                  .getNodes()
                  .stream()
                  .filter(node1 -> node1.equals(targetNode))
                  .forEach(node1 -> setNodeProperty(input, node1));
            }
          }
        }
        Cluster updatedCluster = new Cluster(stripes);
        logger.debug("Updated cluster topology: " + updatedCluster);

        try (MultiDiagnosticServiceConnection multiConnection = connectionFactory.createConnection(cluster.getNodeAddresses())) {
          getTopologyServiceStream(multiConnection)
              .map(Tuple2::getT2)
              .forEach(ts -> ts.setCluster(updatedCluster));
          logger.info("Cluster topology update on all nodes successful");
        }
      }
    }

    logger.info("Command successful!\n");
  }

  private void validateNodeParams() {
    HashMap<String, String> paramValueMap = new HashMap<>();
    for (ParsedInput input : parsedInput) {
      if (input.getPropertyName() != null) {
        paramValueMap.put(input.getProperty(), input.getPropertyName() + PARAM_INTERNAL_SEP + input.getValue());
      } else {
        paramValueMap.put(input.getProperty(), input.getValue());
      }
    }
    new NodeParamsValidator(paramValueMap, IParameterSubstitutor.identity()).validate();
  }

  private TopologyService getTopologyService(MultiDiagnosticServiceConnection singleConnection) {
    return singleConnection.getDiagnosticService(node).get().getProxy(TopologyService.class);
  }

  private Stream<Tuple2<InetSocketAddress, TopologyService>> getTopologyServiceStream(MultiDiagnosticServiceConnection connection) {
    return connection.map((address, diagnosticService) -> diagnosticService.getProxy(TopologyService.class));
  }

  private void parse(String configString, String propertyString, Scope cluster, int stripeId, int nodeId, String value) {
    String[] propertyAndName = propertyString.split("\\.");

    if (propertyAndName.length != 1 && propertyAndName.length != 2) {
      throwException("Expected property in the format <property>.<property-name>, but found: %s in input: %s", propertyString, configString);
    } else {
      Collection<String> allOptions = CommonOptions.getAllOptions();
      if (!allOptions.contains(propertyAndName[0])) {
        throwException("Unknown property: %s in input: %s. Valid properties are: %s", propertyAndName[0], configString, allOptions);
      }

      if (propertyAndName.length == 1) {
        parsedInput.add(new ParsedInput(configString, cluster, stripeId, nodeId, propertyAndName[0], null, value));
      } else {
        parsedInput.add(new ParsedInput(configString, cluster, stripeId, nodeId, propertyAndName[0], propertyAndName[1], value));
      }
    }
  }

  private void setNodeProperty(ParsedInput input, Node targetNode) {
    switch (input.getProperty()) {
      case OFFHEAP_RESOURCES:
        targetNode.setOffheapResource(input.getPropertyName(), Measure.parse(input.getValue(), MemoryUnit.class));
        break;
      case DATA_DIRS:
        targetNode.setDataDir(input.getPropertyName(), Paths.get(input.getValue()));
        break;
      case TC_PROPERTIES:
        targetNode.setTcProperty(input.getPropertyName(), input.getValue());
        break;
      case CLIENT_LEASE_DURATION:
        targetNode.setClientLeaseDuration(Measure.parse(input.getValue(), TimeUnit.class));
        break;
      case CLIENT_RECONNECT_WINDOW:
        targetNode.setClientReconnectWindow(Measure.parse(input.getValue(), TimeUnit.class));
        break;
      case SECURITY_SSL_TLS:
      case SECURITY_WHITELIST:
        invokeSetter(methodName(input), targetNode, boolean.class, Boolean.parseBoolean(input.getValue()));
        break;
      case NODE_PORT:
      case NODE_GROUP_PORT:
        invokeSetter(methodName(input), targetNode, int.class, Integer.parseInt(input.getValue()));
        break;
      case NODE_LOG_DIR:
      case SECURITY_DIR:
      case SECURITY_AUDIT_LOG_DIR:
      case NODE_REPOSITORY_DIR:
      case NODE_BACKUP_DIR:
        invokeSetter(methodName(input), targetNode, Path.class, Paths.get(input.getValue()));
        break;
      default:
        invokeSetter(methodName(input), targetNode, String.class, input.getValue());
    }
  }

  private String methodName(ParsedInput input) {
    Matcher matcher = Pattern.compile("-([a-z])").matcher(input.getProperty());
    StringBuffer sb = new StringBuffer();
    while (matcher.find()) {
      matcher.appendReplacement(sb, matcher.group(1).toUpperCase());
    }
    matcher.appendTail(sb);
    return "set" + sb.replace(0, 1, String.valueOf(sb.charAt(0)).toUpperCase()).toString();
  }

  private void invokeSetter(String methodName, Node targetNode, Class<?> clazz, Object value) {
    try {
      Node.class.getDeclaredMethod(methodName, clazz).invoke(targetNode, value);
    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new AssertionError("Could not invoke methodName: " + methodName + " with value: " + value);
    }
  }
}
