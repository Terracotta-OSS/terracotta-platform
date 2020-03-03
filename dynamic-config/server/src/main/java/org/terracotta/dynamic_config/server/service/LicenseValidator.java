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
