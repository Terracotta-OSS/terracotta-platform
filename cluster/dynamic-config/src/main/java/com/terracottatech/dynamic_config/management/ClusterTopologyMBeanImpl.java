/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tc.management.AbstractTerracottaMBean;
import com.tc.management.TerracottaManagement;
import com.terracottatech.dynamic_config.config.Cluster;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UncheckedIOException;
import java.lang.management.ManagementFactory;


public class ClusterTopologyMBeanImpl extends AbstractTerracottaMBean implements ClusterTopologyMBean {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClusterTopologyMBeanImpl.class);
  private static final String MBEAN_NAME = "ClusterTopologyMBean";
  private final Cluster cluster;

  public static void init(Cluster cluster) {
    try {
      ClusterTopologyMBeanImpl clusterTopologyMBean = new ClusterTopologyMBeanImpl(cluster);
      clusterTopologyMBean.register();
    } catch (Exception e) {
      throw new RuntimeException("Failed to register " + MBEAN_NAME, e);
    }
  }

  private ClusterTopologyMBeanImpl(Cluster cluster) throws Exception {
    super(ClusterTopologyMBean.class, false);
    this.cluster = cluster;
  }

  private void register() throws Exception {
    ManagementFactory.getPlatformMBeanServer().registerMBean(this, TerracottaManagement.createObjectName(null, MBEAN_NAME, TerracottaManagement.MBeanDomain.PUBLIC));
    LOGGER.info("Registered " + MBEAN_NAME);
  }

  @Override
  public String getPendingTopology() {
    try {
      return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(cluster);
    } catch (JsonProcessingException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void reset() {
    // Do nothing
  }
}
