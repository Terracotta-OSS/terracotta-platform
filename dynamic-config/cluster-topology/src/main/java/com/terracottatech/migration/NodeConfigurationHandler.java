/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration;

import org.w3c.dom.Node;

import com.terracottatech.migration.util.Pair;

import java.util.Map;

public interface NodeConfigurationHandler {
  /*
  Interface for handlers which need to process a map containing configuration for a set of nodes
   */
  void process(Map<Pair<String, String>, Node> nodeNameNodeConfigMap);
}
