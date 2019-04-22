/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Cluster {
  private final List<Stripe> stripes;

  public Cluster(List<Stripe> stripes) {
    this.stripes = new ArrayList<>(stripes);
  }

  public List<Stripe> getStripes() {
    return Collections.unmodifiableList(stripes);
  }

  @Override
  public String toString() {
    return "Cluster{" +
        "stripes=" + stripes +
        '}';
  }
}
