/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.NomadClientProcess;
import com.terracottatech.nomad.client.NomadDecider;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.results.AllResultsReceiver;

import java.util.List;

public class RecoveryProcess<T> extends NomadClientProcess<Void, T> {
  public RecoveryProcess(List<NomadEndpoint<T>> servers, String host, String user) {
    super(servers, host, user);
  }

  public void recover(RecoveryResultReceiver<T> results) {
    runProcess(
        new RecoveryAllResultsReceiverAdapter<>(results),
        new RecoveryProcessDecider<>(),
        new RecoveryMessageSender<>(servers, host, user),
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
