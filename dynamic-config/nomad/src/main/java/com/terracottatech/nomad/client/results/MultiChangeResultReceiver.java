/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.client.change.ChangeResultReceiver;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class MultiChangeResultReceiver<T> implements ChangeResultReceiver<T> {

  private final Collection<ChangeResultReceiver<T>> changeResultReceivers;

  public MultiChangeResultReceiver(Collection<ChangeResultReceiver<T>> changeResultReceivers) {
    this.changeResultReceivers = changeResultReceivers;
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void endDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverRepeated(server);
    }
  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareFail(server, reason);
    }
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endPrepare();
    }
  }

  @Override
  public void startCommit() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startCommit();
    }
  }

  @Override
  public void committed(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.committed(server);
    }
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.startRollback();
    }
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (ChangeResultReceiver<T> changeResultReceiver : changeResultReceivers) {
      changeResultReceiver.done(consistency);
    }
  }
}
