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

import com.tc.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeoutException;

public class TCVoterImpl implements TCVoter {

  private static final Logger LOGGER = LoggerFactory.getLogger(TCVoterImpl.class);

  protected final String id = UUID.getUUID().toString();
  private final Map<String, ActiveVoter> registeredClusters = new ConcurrentHashMap<>();

  public TCVoterImpl() {
    LOGGER.info("Voter ID: {}", id);
  }

  protected Optional<Properties> getConnectionProperties() {
    return Optional.empty();
  }

  @Override
  public boolean overrideVote(String hostPort) {
    ClientVoterManager voterManager = new ClientVoterManagerImpl(hostPort);
    voterManager.connect(getConnectionProperties());
    boolean override;
    try {
      override = voterManager.overrideVote(id);
    } catch (TimeoutException e) {
      LOGGER.error("Override vote to {} timed-out", hostPort);
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
  public Future<VoterStatus> register(String clusterName, String... hostPorts) {
    CompletableFuture<VoterStatus> voterStatusFuture = new CompletableFuture<>();
    ActiveVoter activeVoter = new ActiveVoter(id, voterStatusFuture, getConnectionProperties(), hostPorts);
    if (registeredClusters.putIfAbsent(clusterName, activeVoter) != null) {
      throw new RuntimeException("Another cluster is already registered with the name: " + clusterName);
    }
    activeVoter.start();
    return voterStatusFuture;
  }

  @Override
  public void deregister(String clusterName) {
    ActiveVoter voter = registeredClusters.remove(clusterName);
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
