/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.model.validation;

import com.terracottatech.License;
import com.terracottatech.LicenseException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.licensing.DefaultLicenseParser;
import com.terracottatech.licensing.LicenseParser;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;

import java.util.Optional;

public class LicenseValidator {
  private static final LicenseParser LICENSE_PARSER = new DefaultLicenseParser();

  public static void validateLicense(Cluster cluster, String licenseFilePath) {
    License license = parse(licenseFilePath);
    long licenseOffHeapLimitInMB = license.getCapabilityLimitMap().get(DefaultLicenseParser.CAPABILITY_OFFHEAP);
    long totalOffHeapInMB =
        bytesToMegaBytes(
            cluster.getStripes()
                .stream()
                .flatMap(stripe -> stripe.getNodes().stream())
                .flatMap(node -> node.getOffheapResources().values().stream())
                .mapToLong(Measure::getQuantity)
                .sum()
        );

    if (totalOffHeapInMB > licenseOffHeapLimitInMB) {
      throw new IllegalArgumentException("Cluster offheap resource is not within the limit of the license." +
          " Provided: " + totalOffHeapInMB + " MB, but license allows: " + licenseOffHeapLimitInMB + " MB only");
    }

    long perStripeOffHeapSizeInMB = licenseOffHeapLimitInMB / cluster.getStripes().size();
    Optional<Measure<MemoryUnit>> configWithHigherOffheap = cluster.getStripes().stream()
        .flatMap(stripe -> stripe.getNodes().stream())
        .flatMap(node -> node.getOffheapResources().values().stream())
        .filter(measure -> bytesToMegaBytes(measure.getQuantity()) > perStripeOffHeapSizeInMB)
        .findAny();

    if (configWithHigherOffheap.isPresent()) {
      throw new IllegalArgumentException("Stripe offheap resource is not within the per-stripe limit" +
          " of the license. Provided: " + bytesToMegaBytes(configWithHigherOffheap.get().getQuantity()) + " MB," +
          " but license allows: " + perStripeOffHeapSizeInMB + " MB only");
    }
  }

  private static License parse(String licenseFilePath) {
    try {
      return LICENSE_PARSER.parse(licenseFilePath);
    } catch (LicenseException e) {
      throw new IllegalArgumentException(e);
    }
  }

  private static long bytesToMegaBytes(long quantity) {
    return MemoryUnit.MB.convert(quantity, MemoryUnit.B);
  }
}
