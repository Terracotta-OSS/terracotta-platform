/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Stripe {
  private final List<Node> nodes;

  public Stripe(List<Node> nodes) {
    this.nodes = new ArrayList<>(nodes);
  }

  public List<Node> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  @Override
  public String toString() {
    return "Stripe{" +
        "nodes=" + nodes +
        '}';
  }
}
