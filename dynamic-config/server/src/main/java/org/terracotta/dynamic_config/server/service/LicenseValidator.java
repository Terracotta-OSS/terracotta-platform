/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.service;

import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;

public class LicenseValidator {

  public static final String CAPABILITY_OFFHEAP = "OffHeap";

  private final Cluster cluster;
  private final License license;

  public LicenseValidator(Cluster cluster, License license) {
    this.cluster = cluster;
    this.license = license;
  }

  public void validate() {
    long licenseOffHeapLimitInMB = license.getLimit(CAPABILITY_OFFHEAP);
    long totalOffHeapInMB = cluster.getStripes().stream()
        .map(stripe -> stripe.getNodes().get(0))
        .flatMap(node -> node.getOffheapResources().values().stream())
        .mapToLong(measure -> measure.getQuantity(MemoryUnit.MB))
        .sum();

    if (totalOffHeapInMB > licenseOffHeapLimitInMB) {
      throw new InvalidLicenseException(
          String.format(
              "Cluster offheap-resource is not within the license limits. Provided: %d MB, but license allows: %d MB only",
              totalOffHeapInMB,
              licenseOffHeapLimitInMB
          )
      );
    }
  }
}
