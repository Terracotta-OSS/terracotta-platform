/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
import org.terracotta.dynamic_config.api.model.Configuration;
import org.terracotta.dynamic_config.api.server.ConfigChangeHandler;
import org.terracotta.server.Server;
import org.terracotta.server.ServerJMX;

import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class ReplicaChangeHandler implements ConfigChangeHandler {
  private static final Logger LOGGER = getLogger(ReplicaChangeHandler.class);
  private final Server server;

  public ReplicaChangeHandler(Server server) {
    this.server = server;
  }

  @Override
  public void apply(Configuration change) {
    Optional<String> value = change.getValue();
    if (value.isEmpty() || "false".equals(value.get())) {
      // returns true only if server is in passive-replica state
      String success = replicaFailoverToActive();
      if ("true".equals(success)) {
        LOGGER.info("Successfully transitioned Replica to Active state");
      } else {
        LOGGER.warn("Failed to transition Replica to Active state");
      }
    }
  }

  private String replicaFailoverToActive() {
    ServerJMX management = server.getManagement();
    String replicaFailoverToActive = management.call("Server", "replicaFailoverToActive", null);
    return validate("Server", "replicaFailoverToActive", replicaFailoverToActive);
  }

  private static String validate(String mBean, String method, String value) {
    if (value == null || value.startsWith("Invalid JMX")) {
      throw new IllegalStateException("mBean call '" + mBean + "#" + method + "' error: " + value);
    }
    return value;
  }
}


