/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.status;

import com.terracottatech.nomad.client.NomadClientProcess;
import com.terracottatech.nomad.client.NomadDecider;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;

import java.time.Clock;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryProcess<T> extends NomadClientProcess<NomadChange, T> {
  public DiscoveryProcess(List<NomadEndpoint<T>> servers, String host, String user, Clock clock) {
    super(servers, host, user, clock);
  }

  public void discover(DiscoverResultsReceiver<T> results) {
    runProcess(
        new DiscoveryAllResultsReceiverAdapter<>(results),
        new DiscoveryProcessDecider<>(),
        new DiscoveryMessageSender<>(servers, host, user, clock),
        null
    );
  }

  @Override
  protected boolean act(AllResultsReceiver<T> results, NomadDecider<T> decider, NomadMessageSender<T> messageSender, NomadChange change) {
    return false; // to return and not go to commit/rollback steps
  }
}
