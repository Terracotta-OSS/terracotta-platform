/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.recovery;

import com.terracottatech.nomad.client.AsyncCaller;
import com.terracottatech.nomad.client.NomadEndpoint;
import com.terracottatech.nomad.client.NomadMessageSender;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.NomadServerMode;

import java.net.InetSocketAddress;
import java.util.List;

public class RecoveryMessageSender<T> extends NomadMessageSender<T> {
  public RecoveryMessageSender(List<NomadEndpoint<T>> servers, String host, String user, AsyncCaller asyncCaller) {
    super(servers, host, user, asyncCaller);
  }

  @Override
  public void discovered(InetSocketAddress server, DiscoverResponse<T> discovery) {
    super.discovered(server, discovery);

    if (discovery.getMode() == NomadServerMode.PREPARED) {
      preparedServers.add(server);

      // getLatestChange() cannot return null if the server is PREPARED
      changeUuid = discovery.getLatestChange().getChangeUuid(); // we won't do anything with changeUuid if it doesn't match across servers
    }
  }
}
