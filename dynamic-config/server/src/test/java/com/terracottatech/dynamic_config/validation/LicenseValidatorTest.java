/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.validation;

import com.terracottatech.License;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.licensing.LicenseConstants;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.MemoryUnit;
import com.terracottatech.utilities.ValidationException;
import org.junit.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class LicenseValidatorTest {
  @Test(expected = ValidationException.class)
  public void testLicenseViolation_1x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 500L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test(expected = ValidationException.class)
  public void testLicenseViolation_2x1() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 1000L);
    License license = new License(capabilityMap, LocalDate.now());
    Cluster cluster = new Cluster(
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB))),
        new Stripe(Node.newDefaultNode("localhost").setOffheapResource("main", Measure.of(512, MemoryUnit.MB)))
    );
    LicenseValidator validator = new LicenseValidator(cluster, license);
    validator.validate();
  }

  @Test(expected = ValidationException.class)
  public void testLicenseViolation_2x2() {
    Map<String, Long> capabilityMap = new HashMap<>();
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 1000L);
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
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 500L);
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
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 1000L);
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
    capabilityMap.put(LicenseConstants.CAPABILITY_OFFHEAP, 1000L);
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