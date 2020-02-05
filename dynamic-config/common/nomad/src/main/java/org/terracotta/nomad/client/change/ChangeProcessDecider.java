/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import org.terracotta.nomad.client.BaseNomadDecider;
import org.terracotta.nomad.client.results.AllResultsReceiver;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.util.UUID;

import static org.terracotta.nomad.server.NomadServerMode.PREPARED;

public class ChangeProcessDecider<T> extends BaseNomadDecider<T> {
  private volatile AllResultsReceiver<T> results;

  @Override
  public void setResults(AllResultsReceiver<T> results) {
    this.results = results;
  }

  @Override
  public boolean isDiscoverSuccessful() {
    return super.isDiscoverSuccessful() && super.isWholeClusterAccepting();
  }

  @Override
  public boolean shouldDoCommit() {
    return isPrepareSuccessful();
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    NomadServerMode mode = discovery.getMode();

    if (mode == PREPARED) {
      // latestChange cannot be null if the server is PREPARED
      ChangeDetails<T> latestChange = discovery.getLatestChange();
      UUID changeUuid = latestChange.getChangeUuid();
      String creationHost = latestChange.getCreationHost();
      String creationUser = latestChange.getCreationUser();
      results.discoverAlreadyPrepared(server, changeUuid, creationHost, creationUser);
    }
  }
}