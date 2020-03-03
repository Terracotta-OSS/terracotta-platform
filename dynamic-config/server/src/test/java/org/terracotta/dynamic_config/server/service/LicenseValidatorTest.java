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

import org.junit.Test;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.MemoryUnit;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.License;
import org.terracotta.dynamic_config.api.model.Node;
import org.terracotta.dynamic_config.api.model.Stripe;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.terracotta.dynamic_config.server.service.LicenseValidator.CAPABILITY_OFFHEAP;

public class LicenseValidatorTest {
  @Test(expected = InvalidLicenseException.class)
  public void testLicenseViolation_1x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 500L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testLicenseViolation_2x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 1000L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB))),
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test(expected = InvalidLicenseException.class)
  public void testLicenseViolation_2x2() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 1000L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)),
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB))
        ),
        new Stripe(
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)),
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB))
        )
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test
  public void testNoLicenseViolation_1x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 500L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test
  public void testNoLicenseViolation_2x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 1000L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB))),
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test
  public void testNoLicenseViolation_2x2() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(CAPABILITY_OFFHEAP, 1000L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB)),
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB))
        ),
        new Stripe(
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB)),
            Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(500, MemoryUnit.MB))
        )
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }
}