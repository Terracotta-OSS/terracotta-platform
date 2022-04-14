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
package org.terracotta.dynamic_config.test_support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.voter.ActiveVoter;
import org.terracotta.voter.ClientVoterManagerImpl;
import org.terracotta.voter.VoterStatus;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * This class encapsulates all the fixes regarding the use of voter in testing,
 * due to a lack of API and reliability
 * <p>
 * Exmaples:
 * <p>
 * Voter API does not allow to listen on vote actions...
 * <p>
 * Voter heartbeats times out (diagnostic call) and then disconnect closes itself: Heart-beating with {} timed-out...
 *
 * @author Mathieu Carbou
 */
public class DcActiveVoter implements Closeable {

  private static final Logger logger = LoggerFactory.getLogger(DcActiveVoter.class);

  private volatile ActiveVoter activeVoter;
  private volatile CompletableFuture<VoterStatus> voterStatus;
  private final Map<String, CountDownLatch> voted = new HashMap<>();
  private final Properties properties = new Properties();
  private volatile boolean expectedClose;

  public DcActiveVoter(String name, String... hostPorts) {
    //properties.setProperty(DiagnosticsFactory.REQUEST_TIMEOUT, "10000");
    init(name, hostPorts);
  }

  private void init(String name, String... hostPorts) {
    this.voted.clear();
    this.voterStatus = new CompletableFuture<>();
    this.activeVoter = new ActiveVoter(
        name,
        voterStatus,
        Optional.of(properties),
        hostPort -> new ClientVoterManagerImpl(hostPort) {
          @Override
          public long vote(String id, long term) throws TimeoutException {
            final long result = super.vote(id, term);
            if (result == 0) {
              voted.computeIfAbsent(hostPort, hp -> new CountDownLatch(1)).countDown();
            }
            return result;
          }

          @Override
          public synchronized void close() {
            super.close();
            if (!expectedClose) {
              logger.error("** WARNING: CLOSE OF ClientVoterManager: {} for: {}", name, hostPort);
            }
          }

          @Override
          public long heartbeat(String id) throws TimeoutException {
            try {
              return super.heartbeat(id);
            } catch (TimeoutException e) {
              logger.error("** WARNING: HEARTBEAT TIMEOUT FROM ClientVoterManager: {} TO: {} TIMED OUT: {}", name, hostPort, e.getMessage(), e);
              throw e;
            }
          }
        }, hostPorts);
  }

  public void resetVotes() {
    voted.clear();
  }

  public void waitForVote(String hostPort) throws InterruptedException {
    voted.computeIfAbsent(hostPort, hp -> new CountDownLatch(1)).await();
  }

  public void startAndAwaitRegistration() {
    activeVoter.start();
    try {
      voterStatus.get().awaitRegistrationWithAll();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new RuntimeException(e);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getCause());
    }
  }

  public void stop() {
    activeVoter.stop();
  }

  public int getKnownHosts() {
    return activeVoter.getHeartbeatFutures().size();
  }

  @Override
  public void close() {
    expectedClose = true;
    activeVoter.close();
  }
}
