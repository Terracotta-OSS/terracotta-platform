/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.AsyncCaller;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClientProcess;
import com.terracottatech.nomad.client.NomadDecider;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.results.AllResultsReceiver;

import java.util.Collection;

public class RecoveryProcess<T> extends NomadClientProcess<Void, T> {
  public RecoveryProcess(Collection<NamedNomadServer<T>> servers, String host, String user, AsyncCaller asyncCaller) {
    super(servers, host, user, asyncCaller);
  }

  public void recover(RecoveryResultReceiver<T> results) {
    runProcess(
        new RecoveryAllResultsReceiverAdapter<>(results),
        new RecoveryProcessDecider<>(),
        new RecoveryMessageSender<>(servers, host, user, asyncCaller),
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
