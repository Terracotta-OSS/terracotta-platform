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
package org.terracotta.management.service.monitoring;

import org.terracotta.management.sequence.SequenceGenerator;
import org.terracotta.monitoring.PlatformClientFetchedEntity;
import org.terracotta.monitoring.PlatformConnectedClient;
import org.terracotta.monitoring.PlatformEntity;
import org.terracotta.monitoring.PlatformServer;
import org.terracotta.monitoring.ServerState;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;

import static org.terracotta.management.service.monitoring.DefaultMonitoringConsumer.SERVERS_PATH;
import static org.terracotta.management.service.monitoring.DefaultMonitoringConsumer.SERVERS_ROOT_NAME;
import static org.terracotta.management.service.monitoring.Utils.array;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;

/**
 * @author Mathieu Carbou
 */
class PlatformMonitoringProducer extends DefaultMonitoringProducer {

  private final Map<Long, DefaultMonitoringProducer> producers;
  private final SequenceGenerator sequenceGenerator;

  PlatformMonitoringProducer(long consumerId, Map<Long, DefaultMonitoringConsumer> consumers, Map<Long, DefaultMonitoringProducer> producers, SequenceGenerator sequenceGenerator) {
    super(consumerId, consumers);
    this.producers = producers;
    this.sequenceGenerator = sequenceGenerator;
  }

  @Override
  public synchronized boolean addNode(PlatformServer caller, String[] parents, String name, Serializable value) {
    boolean ret = super.addNode(caller, parents, name, value);
    if (ret) {
      // hack to install the active server in the tree
      if (PLATFORM_ROOT_NAME.equals(name)) {
        if (currentActiveServer == null) {
          throw new AssertionError("no active found");
        }
        if (currentActiveServer.getServerName() == null) {
          throw new AssertionError("no server name");
        }
        addNode(caller, new String[]{PLATFORM_ROOT_NAME}, SERVERS_ROOT_NAME, null);
        addNode(caller, SERVERS_PATH, currentActiveServer.getServerName(), currentActiveServer);

      } else if (value instanceof PlatformServer) {
        fire(caller, PlatformNotification.Type.SERVER_JOINED, value);

      } else if (value instanceof PlatformEntity) {
        fire(caller, PlatformNotification.Type.SERVER_ENTITY_CREATED, value);

      } else if (value instanceof PlatformConnectedClient) {
        fire(caller, PlatformNotification.Type.CLIENT_CONNECTED, value);

      } else if (value instanceof ServerState) {
        if (parents.length == 1) { // if platform/state => move it to old place
          addNode(caller, new String[]{PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, caller.getServerName()}, "state", value);
        } else {
          PlatformServer server = getValue(parents, PlatformServer.class);
          fire(caller, PlatformNotification.Type.SERVER_STATE_CHANGED, new Serializable[]{server, value});
        }
      } else if (value instanceof PlatformClientFetchedEntity) {
        PlatformClientFetchedEntity fetch = (PlatformClientFetchedEntity) value;
        PlatformConnectedClient client = getValue(array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, fetch.clientIdentifier), PlatformConnectedClient.class);
        PlatformEntity entity = getValue(array(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, fetch.entityIdentifier), PlatformEntity.class);
        fire(caller, PlatformNotification.Type.SERVER_ENTITY_FETCHED, new Serializable[]{client, entity});
      }
    }
    return ret;
  }

  @Override
  public synchronized boolean removeNode(PlatformServer caller, String[] parents, String name) {
    Serializable value = getValueForNode(parents, name, Serializable.class).orElse(null);
    boolean ret = super.removeNode(caller, parents, name);
    if (ret && value != null) {
      if (value instanceof PlatformServer) {
        fire(caller, PlatformNotification.Type.SERVER_LEFT, value);

      } else if (value instanceof PlatformEntity) {
        // removes the eventual producer linked to this entity
        producers.remove(((PlatformEntity) value).consumerID);
        fire(caller, PlatformNotification.Type.SERVER_ENTITY_DESTROYED, value);

      } else if (value instanceof PlatformConnectedClient) {
        fire(caller, PlatformNotification.Type.CLIENT_DISCONNECTED, value);

      } else if (value instanceof PlatformClientFetchedEntity) {
        PlatformClientFetchedEntity fetch = (PlatformClientFetchedEntity) value;
        PlatformConnectedClient client = getValue(array(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, fetch.clientIdentifier), PlatformConnectedClient.class);
        PlatformEntity entity = getValue(array(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, fetch.entityIdentifier), PlatformEntity.class);
        fire(caller, PlatformNotification.Type.SERVER_ENTITY_UNFETCHED, new Serializable[]{client, entity});
      }
    }
    return ret;
  }

  private void fire(PlatformServer caller, PlatformNotification.Type type, Serializable data) {
    pushBestEffortsData(caller, VoltronMonitoringService.PLATFORM_CATEGORY, new DefaultPlatformNotification(sequenceGenerator.next(), type, data));
  }

  private <T extends Serializable> T getValue(String[] path, Class<T> type) {
    return getValueForNode(path, type)
        .orElseThrow(() -> new IllegalStateException("Invalid tree state: missing " + type.getSimpleName() + " at " + Arrays.toString(path) + "\n" + dumpTree()));
  }

}
