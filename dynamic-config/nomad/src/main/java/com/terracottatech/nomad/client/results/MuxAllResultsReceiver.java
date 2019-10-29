/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class MuxAllResultsReceiver<T> implements AllResultsReceiver<T> {
  private List<AllResultsReceiver<T>> receivers;

  public MuxAllResultsReceiver(List<AllResultsReceiver<T>> receivers) {
    this.receivers = receivers;
  }

  @Override
  public void setResults(AllResultsReceiver<T> results) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.setResults(results);
    }
  }

  @Override
  public void startDiscovery(Collection<InetSocketAddress> servers) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(InetSocketAddress server, String reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverFail(server, reason);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Collection<InetSocketAddress> committedServers, Collection<InetSocketAddress> rolledBackServers) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void endDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(InetSocketAddress server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverRepeated(server);
    }

  }

  @Override
  public void discoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(InetSocketAddress server, UUID changeUuid, String creationHost, String creationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(InetSocketAddress server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(InetSocketAddress server, String reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareFail(server, reason);
    }
  }

  @Override
  public void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endPrepare();
    }
  }

  @Override
  public void startTakeover() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startTakeover();
    }
  }

  @Override
  public void takeover(InetSocketAddress server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeover(server);
    }
  }

  @Override
  public void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void takeoverFail(InetSocketAddress server, String reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.takeoverFail(server, reason);
    }
  }

  @Override
  public void endTakeover() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endTakeover();
    }
  }

  @Override
  public void startCommit() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startCommit();
    }
  }

  @Override
  public void committed(InetSocketAddress server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.committed(server);
    }
  }

  @Override
  public void commitFail(InetSocketAddress server, String reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.commitFail(server, reason);
    }
  }

  @Override
  public void commitOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.startRollback();
    }
  }

  @Override
  public void rolledBack(InetSocketAddress server) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(InetSocketAddress server, String reason) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rollbackFail(server, reason);
    }
  }

  @Override
  public void rollbackOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (AllResultsReceiver<T> receiver : receivers) {
      receiver.done(consistency);
    }
  }
}
