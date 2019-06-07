/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import com.terracottatech.nomad.client.Consistency;
import com.terracottatech.nomad.messages.DiscoverResponse;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MuxAllResultsReceiver implements AllResultsReceiver {
  private List<AllResultsReceiver> receivers;

  public MuxAllResultsReceiver(AllResultsReceiver... receivers) {
    this(Arrays.asList(receivers));
  }

  public MuxAllResultsReceiver(List<AllResultsReceiver> receivers) {
    this.receivers = receivers;
  }

  @Override
  public void setResults(AllResultsReceiver results) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.setResults(results);
    }
  }

  @Override
  public void startDiscovery(Set<String> servers) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startDiscovery(servers);
    }
  }

  @Override
  public void discovered(String server, DiscoverResponse discovery) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discovered(server, discovery);
    }
  }

  @Override
  public void discoverFail(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discoverFail(server);
    }
  }

  @Override
  public void discoverClusterInconsistent(UUID changeUuid, Set<String> committedServers, Set<String> rolledBackServers) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discoverClusterInconsistent(changeUuid, committedServers, rolledBackServers);
    }
  }

  @Override
  public void endDiscovery() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endDiscovery();
    }
  }

  @Override
  public void startSecondDiscovery() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startSecondDiscovery();
    }
  }

  @Override
  public void discoverRepeated(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discoverRepeated(server);
    }

  }

  @Override
  public void discoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endSecondDiscovery() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endSecondDiscovery();
    }
  }

  @Override
  public void discoverAlreadyPrepared(String server, UUID changeUuid, String creationHost, String creationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startPrepare(newChangeUuid);
    }
  }

  @Override
  public void prepared(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.prepared(server);
    }
  }

  @Override
  public void prepareFail(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.prepareFail(server);
    }
  }

  @Override
  public void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.prepareOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void prepareChangeUnacceptable(String server, String rejectionReason) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.prepareChangeUnacceptable(server, rejectionReason);
    }
  }

  @Override
  public void endPrepare() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endPrepare();
    }
  }

  @Override
  public void startTakeover() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startTakeover();
    }
  }

  @Override
  public void takeover(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.takeover(server);
    }
  }

  @Override
  public void takeoverOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.takeoverOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void takeoverFail(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.takeoverFail(server);
    }
  }

  @Override
  public void endTakeover() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endTakeover();
    }
  }

  @Override
  public void startCommit() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startCommit();
    }
  }

  @Override
  public void committed(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.committed(server);
    }
  }

  @Override
  public void commitFail(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.commitFail(server);
    }
  }

  @Override
  public void commitOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.commitOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endCommit() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endCommit();
    }
  }

  @Override
  public void startRollback() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.startRollback();
    }
  }

  @Override
  public void rolledBack(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.rolledBack(server);
    }
  }

  @Override
  public void rollbackFail(String server) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.rollbackFail(server);
    }
  }

  @Override
  public void rollbackOtherClient(String server, String lastMutationHost, String lastMutationUser) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.rollbackOtherClient(server, lastMutationHost, lastMutationUser);
    }
  }

  @Override
  public void endRollback() {
    for (AllResultsReceiver receiver : receivers) {
      receiver.endRollback();
    }
  }

  @Override
  public void done(Consistency consistency) {
    for (AllResultsReceiver receiver : receivers) {
      receiver.done(consistency);
    }
  }
}
