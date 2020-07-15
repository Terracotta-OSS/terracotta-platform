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

import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.api.ConfigChangeHandler;
import org.terracotta.dynamic_config.server.api.InvalidConfigChangeException;

import static org.terracotta.dynamic_config.api.model.Setting.NODE_BIND_ADDRESS;
import static org.terracotta.dynamic_config.api.model.Setting.NODE_GROUP_BIND_ADDRESS;
import static org.terracotta.inet.HostAndIpValidator.isValidHost;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv4;
import static org.terracotta.inet.HostAndIpValidator.isValidIPv6;

public class ServerAttributeConfigChangeHandler implements ConfigChangeHandler {

  @Override
  public void validate(NodeContext nodeContext, Configuration change) throws InvalidConfigChangeException {
    if (change.getValue() == null) {
      throw new InvalidConfigChangeException("Operation not supported");//unset not supported
    }

    try {
      if (change.getSetting() == NODE_GROUP_BIND_ADDRESS || change.getSetting() == NODE_BIND_ADDRESS) {
        validateHostOrIp(change.getValue());
      }

      if (change.getSetting() == NODE_BIND_ADDRESS) {
        // When bind-address is set, set the group-bind-address to the same value because platform does it
        nodeContext.getNode().setNodeGroupBindAddress(change.getValue());
      }
    } catch (RuntimeException e) {
      throw new InvalidConfigChangeException(e.getMessage(), e);
    }
  }

  private void validateHostOrIp(String hostOrIp) throws InvalidConfigChangeException {
    if (!isValidHost(hostOrIp) && !isValidIPv4(hostOrIp) && !isValidIPv6(hostOrIp)) {
      throw new InvalidConfigChangeException("bind address should be a valid hostname or IP");
    }
  }
}
