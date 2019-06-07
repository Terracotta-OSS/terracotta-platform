/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.AsyncCaller;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadClientProcess;
import com.terracottatech.nomad.client.NomadDecider;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.results.AllResultsReceiver;

import java.util.Set;
import java.util.UUID;

public class ChangeProcess extends NomadClientProcess<NomadChange> {
  public ChangeProcess(Set<NamedNomadServer> servers, String host, String user, AsyncCaller asyncCaller) {
    super(servers, host, user, asyncCaller);
  }

  public void applyChange(ChangeResultReceiver results, NomadChange change) {
    runProcess(
        new ChangeAllResultsReceiverAdapter(results),
        new ChangeProcessDecider(),
        new ChangeMessageSender(servers, host, user, asyncCaller),
        change
    );
  }

  @Override
  protected boolean act(AllResultsReceiver results, NomadDecider decider, NomadMessageSender messageSender, NomadChange change) {
    UUID changeUuid = UUID.randomUUID();
    messageSender.sendPrepares(results, changeUuid, change);
    return true;
  }
}
