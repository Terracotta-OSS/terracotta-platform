/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.sync;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tc.exception.TCServerRestartException;
import com.tc.exception.TCShutdownServerException;
import com.tc.exception.ZapDirtyDbServerNodeException;
import org.terracotta.dynamic_config.api.model.Cluster;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ClusterActivationNomadChange;
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

import java.util.Collections;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static org.terracotta.json.Json.parse;
import static org.terracotta.json.Json.toJson;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;

public class DynamicConfigurationPassiveSync {
  private final UpgradableNomadServer<NodeContext> nomadServer;

  public DynamicConfigurationPassiveSync(UpgradableNomadServer<NodeContext> nomadServer) {
    this.nomadServer = nomadServer;
  }

  public byte[] getSyncData() {
    try {
      return Codec.encode(nomadServer.getAllNomadChanges());
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  public void sync(byte[] syncData) {
    try {
      List<NomadChangeInfo<NodeContext>> passiveNomadChanges = nomadServer.getAllNomadChanges();
      List<NomadChangeInfo<NodeContext>> activeNomadChanges = Codec.decode(syncData);
      applyNomadChanges(activeNomadChanges, passiveNomadChanges);
    } catch (NomadException e) {
      throw new RuntimeException(e);
    }
  }

  private void applyNomadChanges(List<NomadChangeInfo<NodeContext>> activeNomadChanges, List<NomadChangeInfo<NodeContext>> passiveNomadChanges) throws NomadException {
    int activeInd = 0, passiveInd = 0;
    int activeChangesSize = activeNomadChanges.size();
    int passiveChangesSize = passiveNomadChanges.size();

    if (passiveChangesSize > activeChangesSize) {
      throw new TCShutdownServerException("Passive has more configuration changes");
    }

    boolean restartRequired = false;
    boolean zapDbRequired = false;

    if (newPreActivatedPassiveJoins(activeNomadChanges, passiveNomadChanges)) {
      // detect the case where we have an active server on the stripe
      // - that has just been started pre-activated or activated from CLI
      // - or that has been activated a while ago and its append log contains some changes
      // And we have just started a pre-activated node, that is becoming passive.
      // We want to include the node in the stripe, but only if it has just been activated (it is new)
      // and its topology matches exactly the one from the active
      nomadServer.reset();
      passiveChangesSize = 0;
      passiveNomadChanges = Collections.emptyList();

      restartRequired = true;
      zapDbRequired = true;
    }

    for (; passiveInd < passiveChangesSize; passiveInd++, activeInd++) {
      NomadChangeInfo<NodeContext> passiveChange = passiveNomadChanges.get(passiveInd);
      NomadChangeInfo<NodeContext> activeChange = activeNomadChanges.get(activeInd);
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
      restartRequired |= tryCommittingChanges(activeNomadChanges.get(activeInd++));
    }

    if (restartRequired && zapDbRequired) {
      throw new ZapDirtyDbServerNodeException("Restarting server");
    } else if (restartRequired) {
      throw new TCServerRestartException("Restarting server");
    }
  }

  private boolean newPreActivatedPassiveJoins(List<NomadChangeInfo<NodeContext>> activeNomadChanges, List<NomadChangeInfo<NodeContext>> passiveNomadChanges) {
    if (activeNomadChanges.isEmpty() || passiveNomadChanges.isEmpty()) {
      throw new AssertionError();
    }

    NomadChangeInfo<NodeContext> info = passiveNomadChanges.get(0);
    Cluster passiveTopology = info.getResult().getCluster();

    if (passiveNomadChanges.size() != 1
        || activeNomadChanges.get(0).equals(info)
        || info.getChangeRequestState() != COMMITTED
        || !(info.getNomadChange() instanceof ClusterActivationNomadChange)) {
      // - if passive and active both have the exact same activated config at the append log beginning, the activation comes from the CLI
      // - entry must be committed in case of a pre-activation
      // - there must be 1 entry in case of a pre-activation
      // - entry must be an activation in case of a pre-activation
      return false;
    }

    // lookup active changes (reverse order) to find the latest change in force
    for (int i = activeNomadChanges.size() - 1; i >= 0; i--) {
      NomadChangeInfo<NodeContext> changeInfo = activeNomadChanges.get(i);
      if (changeInfo.getChangeRequestState() == COMMITTED) {
        // we have found the last topology change in the active
        // we check if yje new passive has joined with the exact same committed config on the active
        return changeInfo.getResult().getCluster().equals(passiveTopology);
      }
    }

    // we were not able to find any committed changed on active... weird...
    return false;
  }

  private void sendPrepare(long mutativeMessageCount, UpgradableNomadServer<NodeContext> nomadServer, NomadChangeInfo<NodeContext> nomadChangeInfo) throws NomadException {
    PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, nomadChangeInfo.getCreationHost(), nomadChangeInfo.getCreationUser(), nomadChangeInfo.getCreationTimestamp(), nomadChangeInfo.getChangeUuid(),
        nomadChangeInfo.getVersion(), nomadChangeInfo.getNomadChange());

    AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
    if (!response.isAccepted()) {
      throw new NomadConfigurationException("Prepare message is rejected by Nomad. Reason for rejection is " + response.getRejectionReason().name() + ": " + nomadChangeInfo.toString());
    }
  }

  private boolean tryCommittingChanges(NomadChangeInfo<NodeContext> nomadChangeInfo) throws NomadException {
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

  private boolean fixPreparedChange(NomadChangeInfo<NodeContext> nomadChangeInfo) throws NomadException {
    DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
    long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
    if (discoverResponse.getLatestChange().getState() != PREPARED) {
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
    static byte[] encode(List<NomadChangeInfo<NodeContext>> nomadChanges) {
      return toJson(requireNonNull(nomadChanges)).getBytes(UTF_8);
    }

    static List<NomadChangeInfo<NodeContext>> decode(byte[] encoded) {
      return parse(new String(encoded, UTF_8), new TypeReference<List<NomadChangeInfo<NodeContext>>>() {
      });
    }
  }
}
