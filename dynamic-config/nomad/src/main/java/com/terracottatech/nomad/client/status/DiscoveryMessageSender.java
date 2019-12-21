/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.status;

import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.client.results.CommitResultsReceiver;
import com.terracottatech.nomad.client.results.PrepareResultsReceiver;
import com.terracottatech.nomad.client.results.RollbackResultsReceiver;
import com.terracottatech.nomad.client.results.TakeoverResultsReceiver;

import java.util.List;
import java.util.UUID;

/**
 * @author Mathieu Carbou
 */
public class DiscoveryMessageSender<T> extends NomadMessageSender<T> {
  public DiscoveryMessageSender(List<NomadEndpoint<T>> servers, String host, String user) {
    super(servers, host, user);
  }

  @Override
  public void sendPrepares(PrepareResultsReceiver results, UUID changeUuid, NomadChange change) {
    // ensure we do nothing
  }

  @Override
  public void sendCommits(CommitResultsReceiver results) {
    // ensure we do nothing
  }

  @Override
  public void sendRollbacks(RollbackResultsReceiver results) {
    // ensure we do nothing
  }

  @Override
  public void sendTakeovers(TakeoverResultsReceiver results) {
    // ensure we do nothing
  }
}
