/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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

import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;

/**
 *
 */
public class ClientVoterThread implements AutoCloseable {

  private static final long HEARTBEAT_INTERVAL = 1000L;

  private final ClientVoterManager mgr;
  private final String id;
  private final Properties connectionProperties;
  private final ScheduledExecutorService exec;
  private static final Logger LOGGER = LoggerFactory.getLogger(ClientVoterThread.class);
  
  private volatile ScheduledFuture<?> currentTask;

  public ClientVoterThread(ClientVoterManager mgr, String id, ScheduledExecutorService clock, Properties props) {
    this.mgr = mgr;
    this.id = id;
    this.exec = clock;
    this.connectionProperties = props;
  }

  private boolean register() throws ConnectionException, TimeoutException {
    if (!mgr.isConnected()) {
      mgr.connect(connectionProperties);
    }
    return mgr.register(id);
  }
  
  public ClientVoterManager getVoterManager() {
    return mgr;
  }
  
  @Override
  public void close() {
    if (currentTask != null) {
      currentTask.cancel(true);
    }
    mgr.close();
  }

  public void operate(Consumer<ClientVoterManager> voteRequest) throws ConnectionException, TimeoutException {
    if (currentTask != null && !currentTask.isDone()) {
      throw new IllegalStateException();
    }
    try {
      if (!register()) {
        throw new RuntimeException("unable to register");
      }
      currentTask = exec.scheduleAtFixedRate(() -> {
        try {
          long election = mgr.heartbeat(id);
          if (election < 0) {
            mgr.close();
            voteRequest.accept(mgr);
          } else if (election == ClientVoterManager.HEARTBEAT_RESPONSE) {
            LOGGER.debug("Heart-beat operational with {}", mgr.getTargetHostPort());
          } else {
            voteRequest.accept(mgr);
            LOGGER.debug("Heart-beat election requested with {}", mgr.getTargetHostPort());
          }
        } catch (TimeoutException to) {
          LOGGER.warn("Heart-beating with {} timed-out", mgr.getTargetHostPort());
          close();
          voteRequest.accept(mgr);
        } catch (Exception run) {
          LOGGER.warn("Heart-beating with {} not connected", mgr.getTargetHostPort(), run);
          close();
          voteRequest.accept(mgr);
        } catch (Throwable run) {
          LOGGER.warn("Heart-beating with {} not connected", mgr.getTargetHostPort(), run);
          close();
          voteRequest.accept(mgr);
        }
      }, 0L, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);
      this.operational.complete(null);
    } catch (Throwable t) {
      this.operational.completeExceptionally(t);
    }
  }

  // For testing
  private final CompletableFuture<?> operational = new CompletableFuture<>();

  CompletableFuture<?> operational() {
    return operational;
  }
}
