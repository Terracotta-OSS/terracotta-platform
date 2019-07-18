/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;

public interface ConfigController {

  int getStripeId();

  String getNodeName();

  Measure<MemoryUnit> getOffheapSize(String name) throws ConfigControllerException;

  void setOffheapSize(String name, Measure<MemoryUnit> newOffheapSize) throws ConfigControllerException;
}
