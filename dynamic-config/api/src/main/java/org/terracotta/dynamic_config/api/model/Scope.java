/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model;

public enum Scope {
  /**
   * Setting can be changed per node
   */
  NODE,

  /**
   * Setting can be changed for all nodes of a stripe
   */
  STRIPE,

  /**
   * Setting can be changed for all the nodes of a cluster
   */
  CLUSTER;


  @Override
  public String toString() {
    return name().toLowerCase();
  }
}
