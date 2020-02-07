/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
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