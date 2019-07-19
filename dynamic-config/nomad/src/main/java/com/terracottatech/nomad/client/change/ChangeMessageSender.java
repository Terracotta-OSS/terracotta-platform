/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.AsyncCaller;
import com.terracottatech.nomad.client.NamedNomadServer;
import com.terracottatech.nomad.client.NomadMessageSender;

import java.util.Collection;
import java.util.UUID;

public class ChangeMessageSender extends NomadMessageSender {
  public ChangeMessageSender(Collection<NamedNomadServer> servers, String host, String user, AsyncCaller asyncCaller) {
    super(servers, host, user, asyncCaller);
  }

  @Override
  public void startPrepare(UUID newChangeUuid) {
    super.startPrepare(newChangeUuid);
    this.changeUuid = newChangeUuid;
  }

  @Override
  public void prepared(String server) {
    super.prepared(server);
    preparedServers.add(server);
  }
}
