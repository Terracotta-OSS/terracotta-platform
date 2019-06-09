/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;

import java.util.function.Supplier;

public class ConfigControllerImpl implements ConfigController {

  private final Supplier<String> stripeNameSupplier;
  private final Supplier<String> nodeNameSupplier;

  public ConfigControllerImpl(Supplier<String> nodeNameSupplier, Supplier<String> stripeNameSupplier) {
    this.nodeNameSupplier = nodeNameSupplier;
    this.stripeNameSupplier = stripeNameSupplier;
  }

  @Override
  public String getStripeName() {
    return stripeNameSupplier.get();
  }

  @Override
  public String getNodeName() {
    return nodeNameSupplier.get();
  }

  @Override
  public Measure<MemoryUnit> getOffheapSize(final String name) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void setOffheapSize(String name, Measure<MemoryUnit> newOffheapSize) {
    throw new UnsupportedOperationException();
  }
}
