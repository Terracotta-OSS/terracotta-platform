/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.service.handler;

import com.terracottatech.dynamic_config.handler.ConfigChangeHandler;
import com.terracottatech.dynamic_config.handler.InvalidConfigChangeException;
import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Configuration;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.utilities.Measure;
import com.terracottatech.utilities.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.Attribute;
import javax.management.JMException;
import javax.management.MBeanServer;
import java.lang.management.ManagementFactory;
import java.util.stream.Stream;

import static com.tc.management.beans.L2MBeanNames.TC_SERVER_INFO;

public class ClientReconnectWindowConfigChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientReconnectWindowConfigChangeHandler.class);
  private static final String ATTR_NAME = "ReconnectWindowTimeout";

  @Override
  public Cluster tryApply(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    ensureMBeanAttributeExists(change);

    Cluster updatedCluster = nodeContext.getCluster();
    try {
      Measure.parse(change.getValue(), TimeUnit.class);
      change.apply(updatedCluster);
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
    return updatedCluster;
  }

  @Override
  public boolean apply(Configuration change) {
    int value = (int) Measure.parse(change.getValue(), TimeUnit.class).getQuantity(TimeUnit.SECONDS);
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      mbeanServer.setAttribute(TC_SERVER_INFO, new Attribute(ATTR_NAME, value));
    } catch (JMException e) {
      LOGGER.error("Invoke resulted in exception", e); // log the exception so that server logs get it too
      throw new AssertionError(e);
    }
    return true;
  }

  private void ensureMBeanAttributeExists(Configuration change) throws InvalidConfigChangeException {
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    boolean canCall;
    try {
      canCall = Stream
          .of(mbeanServer.getMBeanInfo(TC_SERVER_INFO).getAttributes())
          .anyMatch(attr -> ATTR_NAME.equals(attr.getName()) && attr.isReadable() && attr.isWritable());
    } catch (JMException e) {
      LOGGER.error("MBeanServer::getMBeanInfo resulted in:", e);
      canCall = false;
    }

    if (!canCall) {
      throw new InvalidConfigChangeException("Unsupported change: " + change);
    }
  }
}
