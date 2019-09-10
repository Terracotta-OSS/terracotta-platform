/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.test.util;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.migration.nomad.RepositoryStructureBuilder;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.messages.TakeoverMessage;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;

import java.nio.file.Path;

public class CommitSkippingRepositoryBuilder extends RepositoryStructureBuilder {

  public CommitSkippingRepositoryBuilder(Path outputFolderPath) {
    super(outputFolderPath);
  }

  @Override
  protected NomadServer<NodeContext> getNomadServer(int stripeId, String nodeName) throws Exception {
    NomadServer<NodeContext> nomadServer = super.getNomadServer(stripeId, nodeName);
    return new NomadServer<NodeContext>() {
      @Override
      public DiscoverResponse<NodeContext> discover() throws NomadException {
        return nomadServer.discover();
      }

      @Override
      public AcceptRejectResponse prepare(PrepareMessage message) throws NomadException {
        return nomadServer.prepare(message);
      }

      @Override
      public AcceptRejectResponse commit(CommitMessage message) {
        return AcceptRejectResponse.accept();
      }

      @Override
      public AcceptRejectResponse rollback(RollbackMessage message) throws NomadException {
        return nomadServer.rollback(message);
      }

      @Override
      public AcceptRejectResponse takeover(TakeoverMessage message) throws NomadException {
        return nomadServer.takeover(message);
      }
    };
  }
}