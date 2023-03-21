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
package org.terracotta.voter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;
import org.terracotta.connection.ConnectionException;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  protected final String id = UUID.randomUUID().toString();
  private final Map<String, VotingGroup> registeredClusters = new ConcurrentHashMap<>();
  private final Properties connectionProperties;

  public TCVoterImpl() {
    this(new Properties());
  }

  public TCVoterImpl(Properties connectionProperties) {
    this.connectionProperties = connectionProperties;
    LOGGER.info("Voter ID: {}", id);
  }

  protected Properties getConnectionProperties() {
    return connectionProperties;
  }

  @Override
  public boolean overrideVote(String hostPort) {
    ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
    boolean override;
    try {
      voterManager.connect(getConnectionProperties());
      override = voterManager.overrideVote(id);
    } catch (TimeoutException e) {
      LOGGER.error("Override vote to {} timed-out", hostPort);
      return false;
    } catch (ConnectionException c) {
      LOGGER.error("Override vote to {} failed to connect", hostPort, c);
      return false;
    }

    if (override) {
      LOGGER.info("Successfully cast an override vote to {}", hostPort);
    } else {
      LOGGER.info("Override vote rejected by {}", hostPort);
    }
    return override;
  }

  @Override
  public VoterStatus register(String clusterName, String... hostPorts) {
    VotingGroup activeVoter = new VotingGroup(id, getConnectionProperties(), hostPorts);
    if (registeredClusters.putIfAbsent(clusterName, activeVoter) != null) {
      throw new RuntimeException("Another cluster is already registered with the name: " + clusterName);
    }
    return activeVoter.start();
  }

  @Override
  public void deregister(String clusterName) {
    VotingGroup voter = registeredClusters.remove(clusterName);
    if (voter != null) {
      try {
        voter.close();
      } catch (Exception exp) {
        throw new RuntimeException(exp);
      }
    } else {
      throw new RuntimeException("A cluster with the given name: " + clusterName + " is not registered with this voter");
    }
  }

}
