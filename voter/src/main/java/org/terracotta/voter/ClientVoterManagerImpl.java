/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2025
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

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.connection.ConnectionException;
import org.terracotta.connection.Diagnostics;
import org.terracotta.connection.DiagnosticsFactory;

public class ClientVoterManagerImpl implements ClientVoterManager {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientVoterManagerImpl.class);

  public static final String REQUEST_TIMEOUT = "Request Timeout";
  public static final String MBEAN_NAME = "VoterManager";

  private final String hostPort;
  Diagnostics diagnostics;

  private volatile boolean voting = false;
  private volatile long generation = INVALID_VOTER_RESPONSE;
  private volatile long lastVotedGeneration = INVALID_VOTER_RESPONSE;

  public ClientVoterManagerImpl(String hostPort) {
    this.hostPort = hostPort;
  }

  @Override
  public String getTargetHostPort() {
    return this.hostPort;
  }

  @Override
  public void connect(Properties connectionProps) throws ConnectionException {
    String[] split = this.hostPort.split(":");
    InetSocketAddress addr = InetSocketAddress.createUnresolved(split[0], Integer.parseInt(split[1]));
    Diagnostics temp = DiagnosticsFactory.connect(addr, connectionProps);
    synchronized (this) {
      if (diagnostics != null) {
        diagnostics.close();
      }
      diagnostics = temp;
    }
    LOGGER.info("Connected to {}", hostPort);
  }

  @Override
  public boolean register(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "registerVoter", id));
    generation = parseResponse(result);
    return isRegistered();
  }

  @Override
  public long heartbeat(String id) throws TimeoutException {
    long time = System.currentTimeMillis();
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "heartbeat", id));
    LOGGER.debug("voting result {} time {} id {} host {} thread {}", result, System.currentTimeMillis() - time, id, hostPort, Thread.currentThread().getName());
    long response = parseResponse(result);
    if (response < 0) {
// any negative should have thrown an exception
      throw new AssertionError();
    }
    if (response == 0) {
      voting = false;
      generation = HEARTBEAT_RESPONSE;
    } else if (!voting && generation == response) {
        //  already zombied for this generation, cannot vote, just heartbeat
        return 0;
    } else {
      voting = true;
      generation = response;
    }
    return response;
  }

  /**
   * Parse the response from the server.
   *
   * The server should return a long based on current state
   * -1 if the voter that is trying to heartbeat is not registered
   * 0 for a valid heartbeat with no voting occuring
   * any positive long means the server is requesting a vote in the
   * generation returned in the long.
   * @param result
   * @return
   */
  private long parseResponse(String result) {
    try {
      long value = Long.parseLong(result);
      if (value < 0) {
        throw new RuntimeException("invalid voter response");
      }
      return value;
    } catch (NumberFormatException ne) {
      generation = INVALID_VOTER_RESPONSE;
      throw new RuntimeException("invalid response from server", ne);
    }
  }

  @Override
  public long vote(String id) throws TimeoutException {
    if (!voting) {
      throw new RuntimeException("not currently voting");
    }
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "vote", id + ":" + generation));
    long value = Long.parseLong(result);
    if (value == HEARTBEAT_RESPONSE) {
      lastVotedGeneration = generation;
    }
    return value;
  }

  @Override
  public boolean overrideVote(String id) {
    String result = diagnostics.invokeWithArg(MBEAN_NAME, "overrideVote", id);
    return Boolean.parseBoolean(result);
  }

  @Override
  public boolean deregisterVoter(String id) throws TimeoutException {
    String result = processInvocation(diagnostics.invokeWithArg(MBEAN_NAME, "deregisterVoter", id));
    return Boolean.parseBoolean(result);
  }

  @Override
  public String getServerState() throws TimeoutException {
    return processInvocation(diagnostics.getState());
  }

  @Override
  public String getServerConfig() throws TimeoutException {
    return processInvocation(diagnostics.getConfig());
  }

  @Override
  public Set<String> getTopology() throws TimeoutException {
    String res = processInvocation(diagnostics.invoke("TopologyMBean", "getTopology"));
    String[] resHostPorts = res.split(",");
    return new HashSet<>(Arrays.asList(resHostPorts));
  }

  String processInvocation(String invocation) throws TimeoutException {
    if (invocation == null) {
      return "UNKNOWN";
    }
    if (invocation.equals(REQUEST_TIMEOUT)) {
      throw new TimeoutException("Request timed out");
    }
    return invocation;
  }

  @Override
  public synchronized void close() {
    try {
      if (this.diagnostics != null) {
        this.diagnostics.close();
        LOGGER.info("Connection closed to {}", hostPort);
      }
    } catch (Throwable t) {
      LOGGER.info("Connection trouble closing", t);
    } finally {
      this.diagnostics = null;
    }
  }

  @Override
  public synchronized boolean isConnected() {
    return this.diagnostics != null;
  }

  @Override
  public boolean isVoting() {
    return voting;
  }

  @Override
  public boolean isRegistered() {
    return generation >= 0;
  }

  @Override
  public long generation() {
    return generation;
  }

  @Override
  public long lastVotedGeneration() {
    return lastVotedGeneration;
  }

  @Override
  public void zombie() {
    LOGGER.debug("Zombied {} for generation {}", getTargetHostPort(), generation);
    voting = false;
  }

  @Override
  public String toString() {
    return "ClientVoterManagerImpl{" + "hostPort=" + hostPort + ", connection=" + diagnostics + '}';
  }

  @Override
  public long getRegisteredVoterCount() throws TimeoutException {
    String result = processInvocation(diagnostics.invoke(MBEAN_NAME, "getRegisteredVoters"));
    return Long.parseLong(result);
  }

  @Override
  public long getRegisteredVoterLimit() throws TimeoutException {
    String result = processInvocation(diagnostics.invoke(MBEAN_NAME, "getVoterLimit"));
    return Long.parseLong(result);
  }
}
