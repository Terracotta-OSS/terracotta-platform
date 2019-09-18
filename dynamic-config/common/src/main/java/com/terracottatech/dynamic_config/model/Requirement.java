/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model;

/**
 * @author Mathieu Carbou
 */
public enum Requirement {

  /**
   * Setting change needs a restart
   */
  RESTART,

  /**
   * Setting change needs only active servers to be online plus eventually some passive servers, but not all
   */
  ACTIVES_ONLINE,

  /**
   * Setting change needs all nodes online (active and passives)
   */
  ALL_NODES_ONLINE,
}
