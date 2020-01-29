/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.status;

import com.terracottatech.nomad.client.BaseNomadDecider;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryProcessDecider<T> extends BaseNomadDecider<T> {
  @Override
  public boolean shouldDoCommit() {
    return false;
  }

  @Override
  public boolean shouldDoRollback() {
    return false;
  }
}
