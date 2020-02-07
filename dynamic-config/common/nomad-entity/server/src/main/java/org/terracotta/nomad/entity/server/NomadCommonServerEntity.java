/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.entity.server;

import org.terracotta.entity.CommonServerEntity;
import org.terracotta.nomad.entity.common.NomadEntityMessage;
import org.terracotta.nomad.entity.common.NomadEntityResponse;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.MutativeMessage;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

public class NomadCommonServerEntity<T> implements CommonServerEntity<NomadEntityMessage, NomadEntityResponse> {

  private final NomadServer<T> nomadServer;

  public NomadCommonServerEntity(NomadServer<T> nomadServer) {
    this.nomadServer = nomadServer;
  }

  @Override
  public final void createNew() {
  }

  @Override
  public final void destroy() {
  }

  protected AcceptRejectResponse processMessage(MutativeMessage nomadMessage) throws NomadException {
    AcceptRejectResponse response;
    if (nomadMessage instanceof CommitMessage) {
      response = nomadServer.commit((CommitMessage) nomadMessage);
    } else if (nomadMessage instanceof RollbackMessage) {
      response = nomadServer.rollback((RollbackMessage) nomadMessage);
    } else if (nomadMessage instanceof TakeoverMessage) {
      response = nomadServer.takeover((TakeoverMessage) nomadMessage);
    } else if (nomadMessage instanceof PrepareMessage) {
      response = nomadServer.prepare((PrepareMessage) nomadMessage);
    } else {
      throw new IllegalArgumentException("Unsupported Nomad message: " + nomadMessage.getClass().getName());
    }
    return response;
  }
}
