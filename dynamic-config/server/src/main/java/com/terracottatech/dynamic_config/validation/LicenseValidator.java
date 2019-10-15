/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.License;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.licensing.LicenseConstants;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.ValidationException;
import com.terracottatech.utilities.Validator;

public class LicenseValidator implements Validator {
  private final Cluster cluster;
  private final License license;

  public LicenseValidator(Cluster cluster, License license) {
    this.cluster = cluster;
    this.license = license;
  }

  @Override
  public void validate() {
    long licenseOffHeapLimitInMB = license.getLimit(LicenseConstants.CAPABILITY_OFFHEAP);
    long totalOffHeapInMB = cluster.getStripes().stream()
        .map(stripe -> stripe.getNodes().get(0))
        .flatMap(node -> node.getOffheapResources().values().stream())
        .mapToLong(measure -> measure.getQuantity(MemoryUnit.MB))
        .sum();

    if (totalOffHeapInMB > licenseOffHeapLimitInMB) {
      throw new ValidationException(
          String.format(
              "Cluster offheap-resource is not within the license limits. Provided: %d MB, but license allows: %d MB only",
              totalOffHeapInMB,
              licenseOffHeapLimitInMB
          )
      );
    }
  }
}
