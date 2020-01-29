/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.api.model;


import com.tc.classloader.CommonComponent;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


@CommonComponent
public final class License {

  // Mapping between capability name and corresponding limit value
  // Value as 0 means absent and > 0 means present and represents its limit.
  private final Map<String, Long> capabilityLimitMap;

  // Expiry date of the license in UTC.
  private final LocalDate expiryDate;

  public License(Map<String, Long> capabilityLimitMap, LocalDate expiryDate) {
    this.capabilityLimitMap = Collections.unmodifiableMap(new HashMap<>(capabilityLimitMap));
    this.expiryDate = expiryDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public Map<String, Long> getCapabilityLimitMap() {
    return capabilityLimitMap;
  }

  public boolean hasCapability(String capability) {
    Long v = getLimit(capability);
    return v != null && v != 0;
  }

  public Long getLimit(String capability) {
    return capabilityLimitMap.get(capability);
  }

  @Override
  public String toString() {
    return "License{" +
        "capabilityLimitMap=" + capabilityLimitMap +
        ", expiryDate=" + expiryDate +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    License license = (License) o;
    return capabilityLimitMap.equals(license.capabilityLimitMap) && expiryDate.equals(license.expiryDate);
  }

  @Override
  public int hashCode() {
    return 31 * capabilityLimitMap.hashCode() + expiryDate.hashCode();
  }
}
