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
package org.terracotta.dynamic_config.server.service.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.common.struct.Measure;
import org.terracotta.common.struct.TimeUnit;
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.service.ConfigChangeHandler;
import org.terracotta.dynamic_config.api.service.InvalidConfigChangeException;

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
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Invalid change: " + change);
    }

    ensureMBeanAttributeExists(change);

    try {
      Measure.parse(change.getValue(), TimeUnit.class);
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  @Override
  public void apply(Configuration change) {
    int value = (int) Measure.parse(change.getValue(), TimeUnit.class).getQuantity(TimeUnit.SECONDS);
    MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
    try {
      mbeanServer.setAttribute(TC_SERVER_INFO, new Attribute(ATTR_NAME, value));
    } catch (JMException e) {
      LOGGER.error("Invoke resulted in exception", e); // log the exception so that server logs get it too
      throw new AssertionError(e);
    }
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
