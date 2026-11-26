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
package org.terracotta.platform.metrics.voltron;

import com.tc.spi.Guardian;
import java.io.Serializable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import org.terracotta.diagnostic.model.LogicalServerState;
import org.terracotta.event.EventService;
import org.terracotta.monitoring.IStripeMonitoring;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformMonitoringConstants;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.PlatformSystem;
import org.terracotta.monitoring.ServerState;
import org.terracotta.platform.metrics.metric.Counter;
import org.terracotta.platform.metrics.metric.Metric;
import org.terracotta.platform.metrics.metric.MetricService;
import org.terracotta.server.Server;

/**
 *
 */
public class MonitoringMetricCollector implements VoltronMetricCollector, IStripeMonitoring, Guardian {
  private final Server env;
  private PlatformServer self;
  private final EnumMap<EventService.Event, Counter> counters = new EnumMap<>(EventService.Event.class);
  private final AtomicLong connections = new AtomicLong();
  private final AtomicLong clients = new AtomicLong();
  private final AtomicLong voters = new AtomicLong();
  private final AtomicLong fetches = new AtomicLong();

  private final BiConsumer<PlatformServer, String>  stateCheck;
  private final Map<String, LogicalServerState> peers = new ConcurrentHashMap<>();

  private InvokeStateHelper invokeState;

  public MonitoringMetricCollector(Server env, BiConsumer<PlatformServer,String> trigger) {
    this.env = env;
    for (EventService.Event e : EventService.Event.values()) {
      counters.put(e, new Counter());
    }
    this.stateCheck = trigger;
  }

  @Override
  public void count(EventService.Event event, String type) {
    // not used
  }

  @Override
  public Optional<Metric> getGauge(MetricService.Metric metric) {
    switch (metric) {
      case CLIENTS -> {
        return Optional.of(clients::get);
      }
      case CONNECTIONS -> {
        return Optional.of(connections::get);
      }
      case ENTITY_FETCHES -> {
        return Optional.of(fetches::get);
      }
      case PEERS -> {
        return Optional.of(peers::size);
      }
      case VOTERS -> {
        return Optional.of(voters::get);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Metric> getCounter(EventService.Event event) {
    return Optional.ofNullable(counters.get(event));
  }

  @Override
  public Optional<Metric> getCounter(EventService.Event event, String type) {
    // this is only used for onvoke counts so query the state
    return Optional.ofNullable(()->invokeState.getTypeCount(type));
  }

  @Override
  public Map<String, LogicalServerState> getPeers() {
    return Collections.unmodifiableMap(peers);
  }

  @Override
  public void serverDidBecomeActive(PlatformServer ps) {
    self = ps;
    peers.put(ps.getHostName() + ":" + ps.getGroupPort(), LogicalServerState.ACTIVE);
    stateCheck.accept(ps, PlatformMonitoringConstants.SERVER_STATE_ACTIVE);
  }

  @Override
  public void serverDidJoinStripe(PlatformServer ps) {
    peers.put(ps.getHostName() + ":" + ps.getGroupPort(), LogicalServerState.UNKNOWN);
  }

  @Override
  public void serverDidLeaveStripe(PlatformServer ps) {
    peers.remove(ps.getHostName() + ":" + ps.getGroupPort());
  }

  @Override
  public boolean addNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    /* only collect local info */
    boolean added = false;
    if ( isLocalEvent(sender) ) {
      added = handleLocalServerEvent(parents, name, value);
    }
    // special case to handle state events from all servers
    if (parents.length == 1 && parents[0].equals(PlatformMonitoringConstants.PLATFORM_ROOT_NAME) &&
            name.equals(PlatformMonitoringConstants.STATE_NODE_NAME)) {
      /*  handle passive state  */
        stateCheck.accept(sender, value.toString());
        peers.put(sender.getHostName() + ":" + sender.getGroupPort(), LogicalServerState.parse(((ServerState)value).getState()));
        added = true;
    }
    return added;
  }

  private boolean isLocalEvent(PlatformServer server) {
    return self == null /* passive */ || self.equals(server); /* active */
  }

  private boolean handleLocalServerEvent(String[] parents, String name, Serializable value) {
    if (parents.length > 0 && parents[0].equals(PlatformMonitoringConstants.PLATFORM_ROOT_NAME)) {
      if (parents.length > 1) {
        switch (parents[1]) {
          case PlatformMonitoringConstants.FETCHED_ROOT_NAME -> {
            counters.get(EventService.Event.ENTITY_FETCH).increment();
            fetches.incrementAndGet();
          }
          case PlatformMonitoringConstants.CLIENTS_ROOT_NAME -> {
// only capture the add of the client node, not extra properties
            if (parents.length == 2 && value instanceof PlatformConnectedClient) {
              counters.get(EventService.Event.CLIENT_JOIN).increment();
              clients.incrementAndGet();
            }
          }
          case PlatformMonitoringConstants.CONNECTIONS_ROOT_NAME -> {
            counters.get(EventService.Event.CONNECTION_OPEN).increment();
            connections.incrementAndGet();
          }
          case PlatformMonitoringConstants.VOTERS_ROOT_NAME -> {
            counters.get(EventService.Event.VOTER_JOIN).increment();
            voters.incrementAndGet();
          }
          default -> {
            return false;
          }
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public boolean removeNode(PlatformServer sender, String[] parents, String name) {
    /* only collect local info */
    if ( isLocalEvent(sender) ) {
      if (parents.length > 0 && parents[0].equals(PlatformMonitoringConstants.PLATFORM_ROOT_NAME)) {
        switch (parents[1]) {
          case PlatformMonitoringConstants.FETCHED_ROOT_NAME -> {
            counters.get(EventService.Event.ENTITY_RELEASE).increment();
            fetches.decrementAndGet();
          }
          case PlatformMonitoringConstants.CLIENTS_ROOT_NAME -> {
            counters.get(EventService.Event.CLIENT_LEAVE).increment();
            clients.decrementAndGet();
          }
          case PlatformMonitoringConstants.CONNECTIONS_ROOT_NAME -> {
            counters.get(EventService.Event.CONNECTION_CLOSE).increment();
            connections.decrementAndGet();
          }
          case PlatformMonitoringConstants.VOTERS_ROOT_NAME -> {
            counters.get(EventService.Event.VOTER_LEAVE).increment();
            voters.decrementAndGet();
          }
          default -> {
            return false;
          }
        }
      }
      return true;
    } else {
      return false;
    }
  }

  @Override
  public void pushBestEffortsData(PlatformServer ps, String string, Serializable srlzbl) {

  }

  @Override
  public boolean validate(Op op, Properties prprts) {
    return true;
  }

  public void snapshot() {
    invokeState = new InvokeStateHelper(env.queryClusterState(PlatformSystem.ENTITY_MANAGER));
  }

  @Override
  public void replayNode(PlatformServer sender, String[] parents, String name, Serializable value) {
    // don't replay on failover
  }

  @Override
  public boolean requiresLocalDeliveryOnPassive() {
    return true;
  }
}
