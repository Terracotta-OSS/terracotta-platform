/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.NomadClientProcess;
import com.terracottatech.nomad.client.NomadDecider;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.results.AllResultsReceiver;

import java.util.List;
import java.util.UUID;

public class ChangeProcess<T> extends NomadClientProcess<NomadChange, T> {
  public ChangeProcess(List<NomadEndpoint<T>> servers, String host, String user) {
    super(servers, host, user);
  }

  public void applyChange(ChangeResultReceiver<T> results, NomadChange change) {
    runProcess(
        new ChangeAllResultsReceiverAdapter<>(results),
        new ChangeProcessDecider<>(),
        new ChangeMessageSender<>(servers, host, user),
        change
    );
  }

  @Override
  protected boolean act(AllResultsReceiver<T> results, NomadDecider<T> decider, NomadMessageSender<T> messageSender, NomadChange change) {
    UUID changeUuid = UUID.randomUUID();
    messageSender.sendPrepares(results, changeUuid, change);
    return true;
  }
}
