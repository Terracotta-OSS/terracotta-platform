/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.config_provider;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.server.nomad.NomadConfigurationException;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.messages.RollbackMessage;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadChangeInfo;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.UpgradableNomadServer;

import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.terracotta.json.Json.parse;
import static org.terracotta.json.Json.toJson;

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
      throw new TCShutdownServerException("Passive has more configuration changes");
    }

    boolean restartRequired = false;

    for (; passiveInd < passiveChangesSize; passiveInd++, activeInd++) {
      NomadChangeInfo passiveChange = passiveNomadChanges.get(passiveInd);
      NomadChangeInfo activeChange = activeNomadChanges.get(activeInd);
      if (!passiveChange.equals(activeChange)) {
        // if the change is not the same, we check if this is because the latest change on the passive server is PREPARED and
        // on the active server it has been rolled back or committed. If yes, we do not prevent the sync, because it will fix
        // the last change on the passive server following a partial commit or rollback
        if (passiveInd != passiveChangesSize - 1
            || !passiveChange.getChangeUuid().equals(activeChange.getChangeUuid())
            || passiveChange.getChangeRequestState() != ChangeRequestState.PREPARED
            || activeChange.getChangeRequestState() == ChangeRequestState.PREPARED) {
          throw new TCShutdownServerException("Passive cannot sync because the configuration change history does not match: no match on active for this change on passive:" + passiveChange);
        } else {
          restartRequired |= fixPreparedChange(activeChange);
        }
      }
    }

    //Apply all the remaining changes from active
    while (activeInd < activeChangesSize) {
      restartRequired |= tryCommitingChanges(activeNomadChanges.get(activeInd++));
    }

    if (restartRequired) {
      throw new TCServerRestartException("Restarting server");
    }
  }

  private void sendPrepare(long mutativeMessageCount, UpgradableNomadServer<NodeContext> nomadServer, NomadChangeInfo nomadChangeInfo) throws NomadException {
    PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid(),
        nomadChangeInfo.getVersion(), nomadChangeInfo.getNomadChange());

    AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
    if (!response.isAccepted()) {
      throw new NomadConfigurationException("Prepare message is rejected by Nomad. Reason for rejection is " + response.getRejectionReason().name() + ": " + nomadChangeInfo.toString());
    }
  }

  private boolean tryCommitingChanges(NomadChangeInfo nomadChangeInfo) throws NomadException {
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    long mutativeMessageCount = discoverResponse.getMutativeMessageCount();

    switch (nomadChangeInfo.getChangeRequestState()) {
      case PREPARED:
        throw new TCShutdownServerException("Active has some PREPARED configuration changes that are not yet committed.");
      case COMMITTED:
        sendPrepare(mutativeMessageCount, nomadServer, nomadChangeInfo);
        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid());

        AcceptRejectResponse commitResponse = nomadServer.commit(commitMessage);
        if (!commitResponse.isAccepted()) {
          throw new NomadConfigurationException("Unexpected commit failure. " +
              "Reason for failure is " + commitResponse.getRejectionReason() + ": " + commitResponse.getRejectionMessage() + ". " +
              "Change:" + nomadChangeInfo.toString());
        }
        return true;
      case ROLLED_BACK:
        sendPrepare(mutativeMessageCount, nomadServer, nomadChangeInfo);
        nextMutativeMessageCount = mutativeMessageCount + 1;
        RollbackMessage rollbackMessage = new RollbackMessage(nextMutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid());

        AcceptRejectResponse rollbackResponse = nomadServer.rollback(rollbackMessage);
        if (!rollbackResponse.isAccepted()) {
          throw new NomadConfigurationException("Unexpected rollback failure. " +
              "Reason for failure is " + rollbackResponse.getRejectionReason() + ": " + rollbackResponse.getRejectionMessage() + ". " +
              "Change:" + nomadChangeInfo.toString());
        }
        return false;
      default:
        throw new TCShutdownServerException("Invalid Nomad Change State: " + nomadChangeInfo.getChangeRequestState());
    }
  }

  private boolean fixPreparedChange(NomadChangeInfo nomadChangeInfo) throws NomadException {
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
    if (discoverResponse.getLatestChange().getState() != ChangeRequestState.PREPARED) {
      throw new AssertionError("Expected PREPARED state in change " + discoverResponse.getLatestChange());
    }

    switch (nomadChangeInfo.getChangeRequestState()) {
      case COMMITTED: {
        CommitMessage message = new CommitMessage(mutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid());
        AcceptRejectResponse response = nomadServer.commit(message);
        if (!response.isAccepted()) {
          throw new NomadConfigurationException("Unexpected commit failure. " +
              "Reason for failure is " + response.getRejectionReason() + ": " + response.getRejectionMessage() + ". " +
              "Change:" + nomadChangeInfo.toString());
        }
        return true;
      }
      case ROLLED_BACK: {
        RollbackMessage message = new RollbackMessage(mutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid());
        AcceptRejectResponse response = nomadServer.rollback(message);
        if (!response.isAccepted()) {
          throw new NomadConfigurationException("Unexpected rollback failure. " +
              "Reason for failure is " + response.getRejectionReason() + ": " + response.getRejectionMessage() + ". " +
              "Change:" + nomadChangeInfo.toString());
        }
        return false;
      }
      default:
        throw new TCShutdownServerException("Invalid Nomad Change State: " + nomadChangeInfo.getChangeRequestState());
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
