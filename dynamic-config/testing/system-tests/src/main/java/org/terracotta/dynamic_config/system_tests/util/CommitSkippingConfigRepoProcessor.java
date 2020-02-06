/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.system_tests.util;

import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.conversion.ConfigRepoProcessor;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.messages.TakeoverMessage;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;

import java.nio.file.Path;

public class CommitSkippingConfigRepoProcessor extends ConfigRepoProcessor {

  public CommitSkippingConfigRepoProcessor(Path outputFolderPath) {
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