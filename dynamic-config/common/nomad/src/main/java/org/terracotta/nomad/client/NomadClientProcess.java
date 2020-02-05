/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.client.results.CommitRollbackResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.MuxAllResultsReceiver;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

public abstract class NomadClientProcess<C, R> {
  protected final List<NomadEndpoint<R>> servers;
  protected final String host;
  protected final String user;
  protected final Clock clock;

  public NomadClientProcess(List<NomadEndpoint<R>> servers, String host, String user, Clock clock) {
    this.servers = servers;
    this.host = host;
    this.user = user;
    this.clock = clock;
  }

  protected abstract boolean act(AllResultsReceiver<R> results, NomadDecider<R> decider, NomadMessageSender<R> messageSender, C data);

  @SuppressWarnings("unchecked")
  protected void runProcess(AllResultsReceiver<R> results, NomadDecider<R> decider, NomadMessageSender<R> messageSender, C data) {
    ClusterConsistencyChecker<R> consistencyChecker = new ClusterConsistencyChecker<>();
    results = wrap(Arrays.asList(results, decider, messageSender, consistencyChecker));

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

  private AllResultsReceiver<R> wrap(List<AllResultsReceiver<R>> resultsReceivers) {
    AllResultsReceiver<R> muxResults = new MuxAllResultsReceiver<>(resultsReceivers);

    for (AllResultsReceiver<R> resultsReceiver : resultsReceivers) {
      resultsReceiver.setResults(muxResults);
    }

    return muxResults;
  }

  private boolean doubleDiscovery(DiscoverResultsReceiver<R> results, NomadDecider<R> decider, NomadMessageSender<R> messageSender, ClusterConsistencyChecker<R> consistencyChecker) {
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

  private void commitOrRollback(CommitRollbackResultsReceiver results, NomadDecider<R> decider, NomadMessageSender<R> messageSender) {
    if (decider.shouldDoCommit()) {
      messageSender.sendCommits(results);
      results.done(decider.getConsistency());

    } else if (decider.shouldDoRollback()) {
      messageSender.sendRollbacks(results);
      results.done(decider.getConsistency());

    } else {
      results.cannotDecideOverCommitOrRollback();
    }
  }
}