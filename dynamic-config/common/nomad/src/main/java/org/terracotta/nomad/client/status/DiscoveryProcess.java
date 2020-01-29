/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.status;

import org.terracotta.nomad.client.NomadClientProcess;
import org.terracotta.nomad.client.NomadDecider;
import org.terracotta.nomad.client.NomadEndpoint;
import org.terracotta.nomad.client.NomadMessageSender;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;

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
