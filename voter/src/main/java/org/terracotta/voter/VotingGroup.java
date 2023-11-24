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
import org.terracotta.connection.ConnectionException;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * VotingGroup votes only for the active it originally connects to.  If the active disconnects,
 * vote for the next server requesting vote.  If the server with the new vote becomes the new active,
 * continue voting for that active.  If the server with the new vote does not become active
 * in a reasonable amount of time, disconnect from all server and hunt for the new active until found.
 */
public class VotingGroup implements AutoCloseable {

  private static final Logger LOGGER = LoggerFactory.getLogger(VotingGroup.class);

  public final static String ACTIVE_COORDINATOR = "ACTIVE-COORDINATOR";
  public final static String PASSIVE_STANDBY = "PASSIVE-STANDBY";
  private static final Set<String> REGISTERABLE_STATES = new HashSet<>(Arrays.asList(ACTIVE_COORDINATOR,PASSIVE_STANDBY));
  private static final long REG_RETRY_INTERVAL = 5000L;
  private static final long DEFAULT_TOPOLOGY_FETCH_TIME = 30000L;
  public static final String TOPOLOGY_FETCH_TIME_PROPERTY = "org.terracotta.voter.topology.fetch.interval";
  private static final long TOPOLOGY_FETCH_INTERVAL = Long.getLong(TOPOLOGY_FETCH_TIME_PROPERTY, DEFAULT_TOPOLOGY_FETCH_TIME);

  private final Thread voter;
  private final String id;
  private ClientVoterManager voteOwner;
  private final Map<String, ClientVoterThread> nodes = new ConcurrentHashMap<>();
  private final ScheduledExecutorService sharedExecutor;
  private final boolean[] pollingSleepTimer = new boolean[1];

  public VotingGroup(String id, String... hostPorts) {
    this(id, new Properties(), hostPorts);
  }

  public VotingGroup(String id, Properties connectionProps, String... hostPorts) {
    this(id, connectionProps, ClientVoterManagerImpl::new, hostPorts);
  }

  VotingGroup(String id, Properties connectionProps, Function<String, ClientVoterManager> factory, String... hostPorts) {
    this.id = id;
    this.sharedExecutor = Executors.newScheduledThreadPool(hostPorts.length);
    this.voter = voterThread(connectionProps, factory, hostPorts);
  }
  
  public VoterStatus start() {
    this.voter.start();
    return status;
  }

  private Thread voterThread(Properties connectionProps,  Function<String, ClientVoterManager> factory, String... hostPorts) {
    return new Thread(() -> {
      String[] targets = Arrays.copyOf(hostPorts, hostPorts.length);
      while (!sharedExecutor.isShutdown()) {
        setTargets(targets);
        Thread.currentThread().setName("VoterThread[" + String.join(",", targets) + "]");
        List<? extends ClientVoterManager> serverList = Stream.of(targets).map(factory).collect(Collectors.toList());
        LOGGER.info("Attempting to register with this active in the group {}", String.join(",", targets));
        if (registerWithActive(id, serverList, connectionProps)) {
          serverList.forEach(mgr->addClientVoterNode(mgr, connectionProps));
          bootstrapped.complete(null);
          targets = pollTopology(targets, factory, connectionProps);
          reset();
        } else {
          //  try updating the list of hosts
          for (ClientVoterManager mgr : serverList) {
            try {
              if (mgr.isConnected()) {
                targets = mgr.getTopology().stream().toArray(String[]::new);
                break;
              }
            } catch (Exception to) {
              LOGGER.info("unable to fetch topology from {}", mgr.getTargetHostPort(), to);
            }
          }
        }
      }
    }, "VoterThread[" + String.join(",", hostPorts) + "]");
  }

  private boolean registerWithActive(String id,
        List<? extends ClientVoterManager> voterManagers, Properties connectionProps) {
    CompletableFuture<ClientVoterManager> registrationLatch = new CompletableFuture<>();
    List<ScheduledFuture<?>> futures = voterManagers.stream().map(voterManager -> sharedExecutor.scheduleAtFixedRate(() -> {
      if (!voterManager.isConnected()) {
        try {
          voterManager.connect(connectionProps);
        } catch (Exception e) {
          LOGGER.warn("unable to connect to server", e);
          return;
        }
      }

      if (!registrationLatch.isDone()) {
        try {
          String serverState = voterManager.getServerState();
          if (serverState.equals(ACTIVE_COORDINATOR)) {
            if (voterManager.register(id)) {
              registrationLatch.complete(voterManager);
            } else {
              StringBuilder message = new StringBuilder();
              message.append(String.format("Registration with %s in state %s failed. ",
                  voterManager.getTargetHostPort(), voterManager.getServerState()));
              long voterLimit = Math.max(0, voterManager.getRegisteredVoterLimit());
              if (voterManager.getRegisteredVoterCount() >= voterLimit) {
                message.append(String.format("Configured voter limit (%d) has already been reached. ", voterLimit));
              }
              message.append("Retrying...");
              LOGGER.warn(message.toString());
            }
          } else {
            LOGGER.info("State of {}: {}. Continuing the search for an active server.", voterManager.getTargetHostPort(), serverState);
          }
        } catch (TimeoutException e) {
          LOGGER.warn("Closing connection to {} due to timeout while registering. Connection will be re-created later.", voterManager.getTargetHostPort());
          voterManager.close();
        } catch (RuntimeException e) {
          LOGGER.error("Closing connection to {} due to unexpected error while registering. Connection will be re-created later. Error: {}", voterManager.getTargetHostPort(), e.getMessage(), e);
          voterManager.close();
        }
      }
    }, 0, REG_RETRY_INTERVAL, TimeUnit.MILLISECONDS)).collect(Collectors.toList());

    LOGGER.info("waiting to get registered with the active in group {}", voterManagers.stream().map(ClientVoterManager::getTargetHostPort).collect(Collectors.joining(",")));
    try {
      ClientVoterManager mgr = registrationLatch.join();
      LOGGER.info("Vote owner state: {}", mgr.getServerState());
      setVoteOwner(mgr);
      return true;
    } catch (Exception e) {
      LOGGER.warn("error registering with active", e);
      return false;
    } finally {
      futures.forEach(f -> f.cancel(true));
    }
  }
  
  private void addClientVoterNode(ClientVoterManager mgr, Properties connectionProps) {
    ClientVoterThread thread = new ClientVoterThread(mgr, id, sharedExecutor, connectionProps);
    ClientVoterThread former = nodes.put(mgr.getTargetHostPort(), thread);
    if (former != null) {
      former.close();
    }
    try {
      mgr.connect(connectionProps);
      if (mgr.isConnected() && REGISTERABLE_STATES.contains(mgr.getServerState())) {
        thread.operate(this::handleVoteRequest);
      } else {
        thread.close();
      }
    } catch (TimeoutException | ConnectionException to) {
      thread.close();
      nodes.remove(mgr.getTargetHostPort());
      LOGGER.info("Unable to register with target {}", mgr.getTargetHostPort(), to);
    } catch (Throwable c) {
      thread.close();
      nodes.remove(mgr.getTargetHostPort());
      LOGGER.info("Unexpected exception.  Unable to register with target {}", mgr.getTargetHostPort(), c);
    }
  }
  
  private synchronized void setVoteOwner(ClientVoterManager mgr) {
    this.voteOwner = mgr;
  }
   
  private synchronized ClientVoterManager getVoteOwner() {
    return this.voteOwner;
  }
  
  private synchronized void handleVoteRequest(ClientVoterManager mgr) {
    try {
      if (voteOwner == null) {
        LOGGER.info("Skipping vote request, voting group is restarting");
      } else if (!mgr.isConnected()) {
        ClientVoterThread t = nodes.remove(mgr.getTargetHostPort());
        if (t != null) {
          t.close();
          if (nodes.isEmpty()) {
            // if there are no nodes connected, the voter is dead and the process needs
            // to start all over again.  break out of topology polling and allow the process to start from the beginning
            setVoteOwner(null);
            notifySleepTimer();
          }
        }
      } else if (mgr == voteOwner) {
        mgr.vote(id);
      } else if (voteOwner.isConnected()) {
        LOGGER.info("Not the vote owner and the owner is still connected, rejecting the vote request from {} for election term {}", mgr.getTargetHostPort(), voteOwner.generation());
        if (voteOwner.isVoting()) {
          // if the owner is voting, this voter must zombie, cannot vote in this generation
          mgr.zombie();
        }
      } else if (mgr.generation() > voteOwner.lastVotedGeneration()) {
        voteOwner.zombie();
        long result = mgr.vote(id);
        setVoteOwner(mgr);
        LOGGER.info("Stole the vote from {}, voting for {} for term: {}, result: {}", voteOwner.getTargetHostPort(), mgr.getTargetHostPort(), voteOwner.generation(), result);
      } else {
        LOGGER.info("Failed to steal the vote from {}, rejecting the vote request from {} for term {}, last voted election: {}", voteOwner.getTargetHostPort(), mgr.getTargetHostPort(), mgr.generation(), voteOwner.generation());
      }
    } catch (TimeoutException to) {
      throw new RuntimeException(to);
    } finally {
      fireVotingListeners(mgr.getTargetHostPort());
    }
  }
  
  private void reset() {
    for (ClientVoterThread t : nodes.values()) {
      t.close();
    }
    nodes.clear();
    setVoteOwner(null);
  }

  public void stop() {
    LOGGER.info("Stopping {}", this);
    reset();
    sharedExecutor.shutdown();
    try {
      sharedExecutor.awaitTermination(30, TimeUnit.SECONDS);
    } catch (InterruptedException ie) {
      LOGGER.info("shutdown interrupted", ie);
    }
    this.voter.interrupt();
    try {
      this.voter.join();
    } catch (InterruptedException ie) {
      LOGGER.info("interrupted in stop", ie);
    }
  }

  private String[] pollTopology(String[] existingTopo, Function<String, ClientVoterManager> factory, Properties connectionProps) {
    ClientVoterManager active = getVoteOwner();
    while (active != null) {
      CompletableFuture<?> pollMark = refreshPollingFuture();
      try {
        Set<String> existingTopology = nodes.keySet();
        Set<String> newTopology = Collections.unmodifiableSet(active.getTopology());
        LOGGER.info("Topology is {}.", existingTopology);
        // only add more registered nodes if the vote owner is active
        if (active.getServerState().equals(ACTIVE_COORDINATOR) && !newTopology.equals(nodes.keySet())) {
          LOGGER.info("New topology detected {}.", newTopology);
          // Start heartbeating with new servers
          Set<String> addedServers = getAddedServers(existingTopology, newTopology);

          addedServers.forEach(server -> addClientVoterNode(factory.apply(server), connectionProps));

          // Do removal of old servers from topology
          Set<String> removedServers = getRemovedServers(existingTopology, newTopology);
          removedServers.forEach(server -> nodes.remove(server).close());
        }
        existingTopo = newTopology.stream().toArray(String[]::new);
        setTargets(existingTopo);
        pollMark.complete(null);
      } catch (TimeoutException | RuntimeException e) {
        pollMark.completeExceptionally(e);
      }
      sleepForTopologyFetchInterval();
      active = getVoteOwner();
    }
    return existingTopo;
  }

  private static Set<String> getRemovedServers(Set<String> existingTopology, Set<String> newTopology) {
    Set<String> res = new HashSet<>(existingTopology);
    res.removeAll(newTopology);
    return res;
  }

  private static Set<String> getAddedServers(Set<String> existingTopology, Set<String> newTopology) {
    Set<String> res = new HashSet<>(newTopology);
    res.removeAll(existingTopology);
    return res;
  }

  private void sleepForTopologyFetchInterval() {
    try {
      synchronized (pollingSleepTimer) {
        if (!pollingSleepTimer[0]) {
          pollingSleepTimer.wait(TOPOLOGY_FETCH_INTERVAL);
        }
        pollingSleepTimer[0] = false;
      }
    } catch (InterruptedException ie) {
      LOGGER.info("Refreshing topology");
      Thread.currentThread().interrupt();
    }
  }
  
  private void notifySleepTimer() {
    synchronized (pollingSleepTimer) {
      pollingSleepTimer[0] = true;
      pollingSleepTimer.notify();
    }
  }

  @Override
  public void close() {
    stop();
  }

  @Override
  public String toString() {
    return "VotingGroup{" + nodes.keySet() + "}";
  }
  
  // Below is all cruft for testing help
  private final List<Consumer<String>> votingListeners = new CopyOnWriteArrayList<>();
  private final CompletableFuture<?> bootstrapped = new CompletableFuture<>();
  private CompletableFuture<?> pollingFuture = new CompletableFuture<>();
  private String[] targets;
  
  public void addVotingListener(Consumer<String> voter) {
    votingListeners.add(voter);
  }
  
  private void fireVotingListeners(String voter) {
    votingListeners.forEach(c->c.accept(voter));
  }

  public Set<String> getConnectedServers() {
    return nodes.keySet();
  }

  public int countConnectedServers() {
    return nodes.size();
  }
  
  private synchronized CompletableFuture<?> refreshPollingFuture() {
    pollingFuture = new CompletableFuture<>();
    notifyAll();
    return pollingFuture;
  }
  
  private synchronized CompletableFuture<?> waitForRefresh() {
    CompletableFuture<?> current = pollingFuture;
    try {
      while (current == pollingFuture) {
        notifySleepTimer();
        this.wait();
      }
      return pollingFuture;
    } catch (InterruptedException ie) {
      throw new RuntimeException(ie);
    }
  }
  
  public CompletableFuture<?> forceTopologyUpdate() {
    return waitForRefresh();
  }
    
  private synchronized void setTargets(String[] nodes) {
    targets = nodes;
  }
  
  public synchronized Set<String> getExistingTopology() {
    return new HashSet<>(Arrays.asList(targets));
  }
  
  private final VoterStatus status = new VoterStatus() {
    @Override
    public boolean isActive() {
      return bootstrapped.isDone() && nodes.values().stream().map(ClientVoterThread::getVoterManager).allMatch(ClientVoterManager::isRegistered);
    }

    @Override
    public void awaitRegistrationWithAll() throws InterruptedException {
      bootstrapped.join();
      // wait for bootstrapped then check all the nodes
      CompletableFuture.allOf(nodes.values().stream().map(ClientVoterThread::operational).toArray(CompletableFuture[]::new)).join();
    }

    @Override
    public void awaitRegistrationWithAll(long timeout, TimeUnit unit) throws InterruptedException, TimeoutException {
      try {
        long elapsed = System.nanoTime();
        bootstrapped.get(timeout, unit);
        elapsed = unit.convert(System.nanoTime() - elapsed, TimeUnit.NANOSECONDS);
        long left = timeout - elapsed;
        if (left <= 0) {
          throw new TimeoutException();
        }
      // wait for bootstrapped then check all the nodes
        CompletableFuture.allOf(nodes.values().stream().map(ClientVoterThread::operational).toArray(CompletableFuture[]::new)).get(left, unit);
      } catch (ExecutionException ee) {
        throw new CompletionException(ee);
      }
    }
  };
}
