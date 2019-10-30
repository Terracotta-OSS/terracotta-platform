/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.NomadConfigurationException;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.RollbackMessage;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadChangeInfo;
import com.terracottatech.nomad.server.UpgradableNomadServer;

import java.util.List;

import static com.terracottatech.utilities.Json.parse;
import static com.terracottatech.utilities.Json.toJson;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

class ConfigurationSyncManager {
  private final UpgradableNomadServer<NodeContext> nomadServer;

  ConfigurationSyncManager(UpgradableNomadServer<NodeContext> nomadServer) {
    this.nomadServer = nomadServer;
  }

  byte[] getSyncData() {
    try {
      return Codec.encode(nomadServer.getAllNomadChanges());
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  void sync(byte[] syncData) {
    try {
      List<NomadChangeInfo> passiveNomadChanges = nomadServer.getAllNomadChanges();
      List<NomadChangeInfo> activeNomadChanges = Codec.decode(syncData);
      applyNomadChanges(activeNomadChanges, passiveNomadChanges);
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyNomadChanges(List<NomadChangeInfo> activeNomadChanges, List<NomadChangeInfo> passiveNomadChanges) throws NomadException {
    int activeInd = 0, passiveInd = 0;
    int activeChangesSize = activeNomadChanges.size();
    int passiveChangesSize = passiveNomadChanges.size();

    if (passiveChangesSize > activeChangesSize) {
      throw new TCShutdownServerException("Passive has more changes");
    }

    while (passiveInd < passiveChangesSize) {
      if (!passiveNomadChanges.get(passiveInd++).equals(activeNomadChanges.get(activeInd++))) {
        throw new TCShutdownServerException("Passive cannot sync because the change history does not match");
      }
    }

    //Apply all the remaining changes from active
    boolean restartRequired = false;
    while (activeInd < activeChangesSize) {
      restartRequired |= tryCommitingChanges(activeNomadChanges.get(activeInd++));
    }

    if (restartRequired) {
      throw new TCServerRestartException("Restarting server");
    }
  }

  private void sendPrepare(long mutativeMessageCount, UpgradableNomadServer<NodeContext> nomadServer, NomadChangeInfo nomadChangeInfo) throws NomadException {
    PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getChangeUuid(),
        nomadChangeInfo.getVersion(), nomadChangeInfo.getNomadChange());

    AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
    if (!response.isAccepted()) {
      throw new NomadConfigurationException("Prepare message is rejected by Nomad. Reason for rejection is "
          + response.getRejectionReason().name() + nomadChangeInfo.toString());
    }
  }

  private boolean tryCommitingChanges(NomadChangeInfo nomadChangeInfo) throws NomadException {
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    long mutativeMessageCount = discoverResponse.getMutativeMessageCount();

    switch (nomadChangeInfo.getChangeRequestState()) {
      case PREPARED:
        throw new TCShutdownServerException("Active has some PREPARED changes that is not yet committed.");
      case COMMITTED:
        sendPrepare(mutativeMessageCount, nomadServer, nomadChangeInfo);
        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getChangeUuid());

        AcceptRejectResponse commitResponse = nomadServer.commit(commitMessage);
        if (!commitResponse.isAccepted()) {
          throw new NomadConfigurationException("Unexpected commit failure. Reason for failure is "
              + commitResponse.getRejectionReason().name() + nomadChangeInfo.toString());
        }
        return true;
      case ROLLED_BACK:
        sendPrepare(mutativeMessageCount, nomadServer, nomadChangeInfo);
        nextMutativeMessageCount = mutativeMessageCount + 1;
        RollbackMessage rollbackMessage = new RollbackMessage(nextMutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getChangeUuid());

        AcceptRejectResponse rollbackResponse = nomadServer.rollback(rollbackMessage);
        if (!rollbackResponse.isAccepted()) {
          throw new NomadConfigurationException("Unexpected rollback failure. Reason for failure is "
              + rollbackResponse.getRejectionReason().name() + nomadChangeInfo.toString());
        }
        return false;
      default:
        throw new TCShutdownServerException("Invalid Nomad Change State");
    }
  }

  static class Codec {
    static byte[] encode(List<NomadChangeInfo> nomadChanges) {
      return toJson(requireNonNull(nomadChanges)).getBytes(UTF_8);
    }

    static List<NomadChangeInfo> decode(byte[] encoded) {
      return parse(new String(encoded, UTF_8), new TypeReference<List<NomadChangeInfo>>() {
      });
    }
  }
}
