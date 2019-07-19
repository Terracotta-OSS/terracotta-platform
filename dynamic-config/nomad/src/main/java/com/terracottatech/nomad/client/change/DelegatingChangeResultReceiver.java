/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class DelegatingChangeResultReceiver implements ChangeResultReceiver {

  private final Collection<ChangeResultReceiver> changeResultReceivers;

  public DelegatingChangeResultReceiver(Collection<ChangeResultReceiver> changeResultReceivers) {
    this.changeResultReceivers = changeResultReceivers;
  }

  public DelegatingChangeResultReceiver(ChangeResultReceiver... changeResultReceivers) {
    this.changeResultReceivers = Arrays.asList(changeResultReceivers);
  }

  @Override
  public void startDiscovery(Set<String> servers) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(String server, DiscoverResponse discovery) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverFail(server);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void endDiscovery() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUuid, String creationHost, String creationUser) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareFail(server);
    }
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endPrepare();
    }
  }

  @Override
  public void startCommit() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startCommit();
    }
  }

  @Override
  public void committed(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.committed(server);
    }
  }

  @Override
  public void commitFail(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitFail(server);
    }
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startRollback();
    }
  }

  @Override
  public void rolledBack(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(String server) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackFail(server);
    }
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (ChangeResultReceiver changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.done(consistency);
    }
  }
}
