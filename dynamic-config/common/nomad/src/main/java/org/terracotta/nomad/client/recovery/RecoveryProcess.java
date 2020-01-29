/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.recovery;

import org.terracotta.nomad.client.NomadClientProcess;
import org.terracotta.nomad.client.NomadDecider;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.server.ChangeRequestState;

import java.time.Clock;
import java.util.List;

public class RecoveryProcess<T> extends NomadClientProcess<Void, T> {
  public RecoveryProcess(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  public void recover(RecoveryResultReceiver<T> results, int expectedNodeCount, ChangeRequestState forcedState) {
    runProcess(
        new RecoveryAllResultsReceiverAdapter<>(results),
        new RecoveryProcessDecider<>(expectedNodeCount, forcedState),
        new RecoveryMessageSender<>(servers, host, user, clock),
        null
    );
  }

  @Override
  protected boolean act(AllResultsReceiver<T> results, NomadDecider<T> decider, NomadMessageSender<T> messageSender, Void data) {
    if (decider.isWholeClusterAccepting()) {
      return false;
    }

    messageSender.sendTakeovers(results);

    return decider.isTakeoverSuccessful();
  }
}
