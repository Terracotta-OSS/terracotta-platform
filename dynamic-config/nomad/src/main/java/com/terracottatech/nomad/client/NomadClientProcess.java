/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.results.AllResultsReceiver;
import com.terracottatech.nomad.client.results.CommitRollbackResultsReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.client.results.MuxAllResultsReceiver;

import java.util.Collection;

public abstract class NomadClientProcess<T> {
  protected final Collection<NamedNomadServer> servers;
  protected final String host;
  protected final String user;
  protected final AsyncCaller asyncCaller;

  public NomadClientProcess(Collection<NamedNomadServer> servers, String host, String user, AsyncCaller asyncCaller) {
    this.servers = servers;
    this.host = host;
    this.user = user;
    this.asyncCaller = asyncCaller;
  }

  protected abstract boolean act(AllResultsReceiver results, NomadDecider decider, NomadMessageSender messageSender, T data);

  protected void runProcess(AllResultsReceiver results, NomadDecider decider, NomadMessageSender messageSender, T data) {
    ClusterConsistencyChecker consistencyChecker = new ClusterConsistencyChecker();
    results = wrap(results, decider, messageSender, consistencyChecker);

    if (!doubleDiscovery(results, decider, messageSender, consistencyChecker)) {
      results.done(decider.getConsistency());
      return;
    }

    if (!act(results, decider, messageSender, data)) {
      results.done(decider.getConsistency());
      return;
    }

    commitOrRollback(results, decider, messageSender);
  }

  private AllResultsReceiver wrap(AllResultsReceiver... resultsReceivers) {
    AllResultsReceiver muxResults = new MuxAllResultsReceiver(resultsReceivers);

    for (AllResultsReceiver resultsReceiver : resultsReceivers) {
      resultsReceiver.setResults(muxResults);
    }

    return muxResults;
  }

  private boolean doubleDiscovery(DiscoverResultsReceiver results, NomadDecider decider, NomadMessageSender messageSender, ClusterConsistencyChecker consistencyChecker) {
    messageSender.sendDiscovers(results);

    if (!decider.isDiscoverSuccessful()) {
      return false;
    }

    messageSender.sendSecondDiscovers(results);

    if (decider.isDiscoverSuccessful()) {
      consistencyChecker.checkClusterConsistency(results);
    }

    results.endSecondDiscovery();

    return decider.isDiscoverSuccessful();
  }

  private void commitOrRollback(CommitRollbackResultsReceiver results, NomadDecider decider, NomadMessageSender messageSender) {
    if (decider.shouldDoCommit()) {
      messageSender.sendCommits(results);
    } else {
      messageSender.sendRollbacks(results);
    }

    results.done(decider.getConsistency());
  }
}
