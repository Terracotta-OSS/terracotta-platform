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
package org.terracotta.dynamic_config.test_support.angela;

import org.junit.runners.model.MultipleFailureException;
import org.terracotta.angela.client.ClientArray;
import org.terracotta.angela.client.ClusterFactory;
import org.terracotta.angela.client.ClusterMonitor;
import org.terracotta.angela.client.Tms;
import org.terracotta.angela.client.Tsa;
import org.terracotta.angela.client.config.ConfigurationContext;
import org.terracotta.angela.common.cluster.Cluster;
import org.terracotta.angela.common.tcconfig.TerracottaServer;
import org.terracotta.port_locking.LockingPortChooser;
import org.terracotta.port_locking.MuxPortLock;
import org.terracotta.port_locking.PortLockingRule;
import org.terracotta.testing.ExternalResource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.stream.IntStream.rangeClosed;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_ACTIVE;
import static org.terracotta.angela.common.TerracottaServerState.STARTED_AS_PASSIVE;

/**
 * @author Mathieu Carbou
 */
public class AngelaRule extends ExternalResource {

  private final Map<String, TerracottaServer> nodes = new ConcurrentHashMap<>();
  private final LockingPortChooser lockingPortChooser;
  private final ConfigurationContext configurationContext;
  private final boolean autoStart;
  private final boolean autoActivate;
  private final Collection<MuxPortLock> locks = new ArrayList<>();

  private ClusterFactory clusterFactory;

  public AngelaRule(LockingPortChooser lockingPortChooser, ConfigurationContext configurationContext, boolean autoStart, boolean autoActivate) {
    this.lockingPortChooser = lockingPortChooser;
    this.configurationContext = configurationContext;
    this.autoStart = autoStart;
    this.autoActivate = autoActivate;

    this.ports = new PortLockingRule(lockingPortChooser, 2 * this.stripes * this.nodesPerStripe);

    .tsaPort(getNodePort(stripeId, nodeId))
        .tsaGroupPort(getNodeGroupPort(stripeId, nodeId))
  }

  @Override
  protected void before() throws Throwable {
    this.clusterFactory = new ClusterFactory(getClass().getSimpleName(), configurationContext);

    if (autoStart) {
      startNodes();
      if (autoActivate) {
        angela.tsa().attachAll();
        angela.tsa().activateAll();
        for (int stripeId = 1; stripeId <= stripes; stripeId++) {
          waitForActive(stripeId);
          waitForPassives(stripeId);
        }
      }
    }
  }

  @Override
  protected void after() throws Throwable {
    List<Throwable> errs = new ArrayList<>(0);
    try {
      this.clusterFactory.close();
    } catch (Throwable e) {
      errs.add(e);
    }
    synchronized (locks) {
      for (MuxPortLock lock : locks) {
        try {
          lock.close();
        } catch (Throwable e) {
          errs.add(e);
        }
      }
    }
    if (!errs.isEmpty()) {
      throw new MultipleFailureException(errs);
    }
  }

  public void startNodes() {
    for (int stripeId = 1; stripeId <= stripes; stripeId++) {
      for (int nodeId = 1; nodeId <= nodesPerStripe; nodeId++) {
        startNode(stripeId, nodeId);
      }
    }
  }

  public void startNode(int stripeId, int nodeId) {
    startNode(getNode(stripeId, nodeId));
  }

  public void startNode(int stripeId, int nodeId, String... cli) {
    startNode(getNode(stripeId, nodeId), cli);
  }

  public void startNode(TerracottaServer node, String... cli) {
    angela.tsa().start(node, cli);
  }

  public void stopNode(int stripeId, int nodeId) {
    angela.tsa().stop(getNode(stripeId, nodeId));
  }

  public TerracottaServer getNode(int stripeId, int nodeId) {
    String key = combine(stripeId, nodeId);
    TerracottaServer server = nodes.get(key);
    if (server == null) {
      throw new IllegalArgumentException("No server for node " + key);
    }
    return server;
  }

  public OptionalInt findActive(int stripeId) {
    return rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> angela.tsa().getState(getNode(stripeId, nodeId)) == STARTED_AS_ACTIVE)
        .findFirst();
  }

  public int[] findPassives(int stripeId) {
    return rangeClosed(1, nodesPerStripe)
        .filter(nodeId -> angela.tsa().getState(getNode(stripeId, nodeId)) == STARTED_AS_PASSIVE)
        .toArray();
  }

  public int getNodePort(int stripeId, int nodeId) {
    if (nodeId > nodesPerStripe) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId + ". Stripes have maximum of " + nodesPerStripe + " nodes.");
    }
    if (nodeId < 1) {
      throw new IllegalArgumentException("Invalid node ID: " + nodeId);
    }
    if (stripeId > stripes) {
      throw new IllegalArgumentException("Invalid stripe ID: " + stripeId + ". There are " + stripes + " stripe(s).");
    }
    if (stripeId < 1) {
      throw new IllegalArgumentException("Invalid stripe ID: " + nodeId);
    }
    //1-1 => 0 and 1
    //1-2 => 2 and 3
    //1-3 => 4 and 5
    //2-1 => 6 and 7
    //2-2 => 8 and 9
    //2-3 => 10 and 11
    return ports.getPorts()[2 * (nodeId - 1) + 2 * nodesPerStripe * (stripeId - 1)];
  }

  protected int getNodeGroupPort(int stripeId, int nodeId) {
    return getNodePort(stripeId, nodeId) + 1;
  }

  protected String combine(int stripeId, int nodesId) {
    return stripeId + "-" + nodesId;
  }

  // delegates

  public Tsa tsa() {return clusterFactory.tsa(); }

  public Cluster cluster() {return clusterFactory.cluster();}

  public Tms tms() {return clusterFactory.tms();}

  public ClientArray clientArray() {return clusterFactory.clientArray();}

  public ClusterMonitor monitor() {return clusterFactory.monitor();}

  private MuxPortLock newPorts(int count) {
    synchronized (locks) {
      MuxPortLock muxPortLock = lockingPortChooser.choosePorts(count);
      locks.add(muxPortLock);
      return muxPortLock;
    }
  }
}
