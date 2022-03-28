/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.dynamic_config.api.model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.System.lineSeparator;

public class License {

  // Mapping between capability name and corresponding limit value
  // Value as 0 means absent and > 0 means present and represents its limit.
  private final Map<String, Long> capabilityLimitMap;

  // Expiry date of the license in UTC.
  private final LocalDate expiryDate;

  // Flag, for each type of license, to indicates if it's active.
  private final Map<String, Boolean> flagsMap;

  public License(Map<String, Long> capabilityLimitMap,
                 LocalDate expiryDate) {
    this(capabilityLimitMap, Collections.emptyMap(), expiryDate);
  }

  public License(Map<String, Long> capabilityLimitMap,
                 Map<String, Boolean> flagsMap,
                 LocalDate expiryDate) {
    this.capabilityLimitMap = Collections.unmodifiableMap(new HashMap<>(capabilityLimitMap));
    this.flagsMap = Collections.unmodifiableMap(new HashMap<>(flagsMap));
    this.expiryDate = expiryDate;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public Map<String, Long> getCapabilityLimitMap() {
    return capabilityLimitMap;
  }

  public Map<String, Boolean> getFlagsMap() {
    return flagsMap;
  }

  public boolean hasCapability(String capability) {
    Long v = getLimit(capability);
    return v != null && v != 0;
  }

  public Long getLimit(String capability) {
    return capabilityLimitMap.get(capability);
  }

  /***
   * Returns first 'true' value of SubscriptionLicense, PerpetualLicense or DatahubLicense
   */
  public String getType() {
    return flagsMap.entrySet().stream().filter(Map.Entry::getValue).findFirst().get().getKey();
  }

  @Override
  public String toString() {
    return "License{" +
        "capabilityLimitMap=" + capabilityLimitMap +
        ", expiryDate=" + expiryDate +
        ", flagsMap=" + flagsMap +
        "}";
  }

  public String toLoggingString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Type: ").append(getType()).append(lineSeparator());
    sb.append("Expiration: ").append(expiryDate == LocalDate.MAX ? "Never" : expiryDate).append(lineSeparator());
    sb.append("Capabilities:").append(lineSeparator());
    Map<String, Long> capabilities = new HashMap<>(capabilityLimitMap);
    capabilities.remove("OffHeap"); // handle this separately
    capabilities.forEach((k,v) -> sb.append(k + ": ").append(hasCapability(k)).append(lineSeparator()));
    sb.append("OffHeap: ").append(getLimit("OffHeap"));
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    License license = (License) o;
    return capabilityLimitMap.equals(license.capabilityLimitMap) &&
        expiryDate.equals(license.expiryDate) &&
        flagsMap.equals(license.flagsMap);
  }

  @Override
  public int hashCode() {
    return Objects.hash(capabilityLimitMap, expiryDate, flagsMap);
  }
}
