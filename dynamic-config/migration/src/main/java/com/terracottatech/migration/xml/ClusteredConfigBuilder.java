/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.xml;

import com.terracottatech.dynamic_config.xml.topology.config.parser.SchemaProvider;
import com.terracottatech.struct.tuple.Tuple2;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClusteredConfigBuilder {

  private static final String TERRACOTTA_CLUSTER_CONFIG_NAMESPACE = SchemaProvider.NAMESPACE_URI.toString();
  private static final String PLUGINS_NODE_NAME = "plugins";
  private static final String CONFIG_NODE_NAME = "config";

  private final Map<Tuple2<Integer, String>, Node> serverNameRootNodeMap;
  private final Map<Tuple2<Integer, String>, Node> hostClonedConfigMap;
  private final Map<Integer, List<String>> stripeServerListMap;

  public ClusteredConfigBuilder(final Map<Tuple2<Integer, String>, Node> serverNameRootNodeMap, final Map<Integer, List<String>> stripeServerListMap) {
    this.serverNameRootNodeMap = serverNameRootNodeMap;
    this.stripeServerListMap = stripeServerListMap;
    this.hostClonedConfigMap = new HashMap<>();
    this.serverNameRootNodeMap.forEach(
        (key, value) -> hostClonedConfigMap.put(key, createInternalTerracottaConfigurationNode(value))
    );
  }

  public void createEntireCluster(String clusterName) {
    serverNameRootNodeMap.entrySet().forEach(
        entry -> createEntireCluster(clusterName, entry)
    );
  }

  private void createEntireCluster(String clusterName, Map.Entry<Tuple2<Integer, String>, Node> hostConfigMapNodeEntry) {

    int stripeId = hostConfigMapNodeEntry.getKey().t1;
    Node clusterNode = createClusterElement(clusterName, stripeId, hostConfigMapNodeEntry.getValue());
    List<Node> stripes = createStripeElementsForOneServer(hostConfigMapNodeEntry.getValue());

    stripes.forEach(clusterNode::appendChild);

    Node configElement = createConfigNode(hostConfigMapNodeEntry.getValue());
    configElement.appendChild(clusterNode);

    //FunctionalWrapper.WrapCheckedConsumer.wrap(this::print).accept(clusterNode);

    Node tcConfigNode = hostConfigMapNodeEntry.getValue().getChildNodes().item(0);

    for (int i = 0; i < tcConfigNode.getChildNodes().getLength(); i++) {
      Node node = tcConfigNode.getChildNodes().item(i);
      if (PLUGINS_NODE_NAME.equals(node.getLocalName())) {
        node.appendChild(configElement);
      }
    }
  }

  private Node createConfigNode(Node rootNode) {
    return ((Document) rootNode).createElement(CONFIG_NODE_NAME);
  }

  private Node createServerElement(Node rootNode, Node serverConfigNode) {
    Node serverConfigElement = createNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:server-config");

    //TODO revisit
    if (serverConfigElement.getOwnerDocument() != serverConfigNode.getOwnerDocument()) {
      serverConfigNode = ((Document) rootNode).importNode(serverConfigNode, true);
    }

    serverConfigElement.appendChild(serverConfigNode);
    return serverConfigElement;
  }

  private Node createClusterElement(String clusterName, int currentStripeId, Node rootNode) {
    Element clusterElement = createNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:cluster");
    clusterElement.setAttribute("currentStripeId", String.valueOf(currentStripeId));
    Node clusterNameElement = createSimpleTextNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:name", clusterName);
    clusterElement.appendChild(clusterNameElement);
    return clusterElement;
  }

  private Node createStripeElement(int stripeId, List<String> serverNames, Node rootNode, Map<Tuple2<Integer, String>, Node> hostClonedConfigMap) {
    Node stripeElement = createNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:stripe");

    serverNames.forEach(serverName -> {
      Node memberElement = createNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:node");
      Node serverConfigElementName = createSimpleTextNode(rootNode, TERRACOTTA_CLUSTER_CONFIG_NAMESPACE, "cl:name", serverName);
      Tuple2<Integer, String> stripeHost = Tuple2.tuple2(stripeId, serverName);
      Node serverConfigElement = createServerElement(rootNode, hostClonedConfigMap.get(stripeHost));
      memberElement.appendChild(serverConfigElementName);
      memberElement.appendChild(serverConfigElement);
      stripeElement.appendChild(memberElement);
    });
    return stripeElement;
  }

  private List<Node> createStripeElementsForOneServer(Node rootNode) {
    List<Node> retList = new ArrayList<>();
    for (Map.Entry<Integer, List<String>> concatServerNamesStripeId : this.stripeServerListMap.entrySet()) {
      Node oneStripeElement = createStripeElement(concatServerNamesStripeId.getKey(), concatServerNamesStripeId.getValue(), rootNode, hostClonedConfigMap);
      retList.add(oneStripeElement);
    }
    return retList;
  }

  private static Element createNode(Node documentRoot, String nameSpace, String nodeName) {
    return XmlUtility.createNode(documentRoot, nameSpace, nodeName);
  }

  private Node createSimpleTextNode(Node rootNode, String nameSpace, String nodeName, String texValue) {
    return XmlUtility.createSimpleTextNode(rootNode, nameSpace, nodeName, texValue);
  }

  private Node createInternalTerracottaConfigurationNode(Node rootElement) {
    return ((Document) rootElement).importNode(rootElement.getChildNodes().item(0), true);
  }
}