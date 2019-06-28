/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.nomad;

import com.terracottatech.dynamic_config.nomad.ConfigMigrationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.dynamic_config.nomad.UpgradableNomadServerFactory;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.migration.NodeConfigurationHandler;
import com.terracottatech.migration.exception.MigrationException;
import com.terracottatech.migration.util.Pair;
import com.terracottatech.migration.util.XmlUtility;
import com.terracottatech.nomad.client.change.NomadChange;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.PotentialApplicationResult;
import com.terracottatech.persistence.sanskrit.SanskritException;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static com.terracottatech.migration.exception.ErrorCode.UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE;

public class RepositoryStructureBuilder implements NodeConfigurationHandler {
  private final Path outputFolderPath;
  private final UUID nomadRequestId;
  private final NomadEnvironment nomadEnvironment;

  public RepositoryStructureBuilder(Path outputFolderPath) {
    this.outputFolderPath = outputFolderPath;
    this.nomadRequestId = UUID.randomUUID();
    this.nomadEnvironment = new NomadEnvironment();
  }

  @Override
  public void process(final Map<Pair<String, String>, Node> nodeNameNodeConfigMap) {
    nodeNameNodeConfigMap.forEach((stripeNameServerName, doc) -> {
      try {
        String xml = XmlUtility.getPrettyPrintableXmlString(doc);
        NomadServer nomadServer = getNomadServer(stripeNameServerName.getOne(), stripeNameServerName.getAnother());
        DiscoverResponse discoverResponse = nomadServer.discover();
        long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
        long nextVersionNumber = discoverResponse.getCurrentVersion() + 1;

        PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), nomadRequestId,
            nextVersionNumber, new ConfigMigrationNomadChange(xml));
        AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
        if (!response.isAccepted()) {
          throw new MigrationException(UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE, "Response code from nomad:" + response.getRejectionReason());
        }

        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, getHost(), getUser(), nomadRequestId);
        nomadServer.commit(commitMessage);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected NomadServer getNomadServer(String nodeName) throws Exception {
    Path nomadRoot = outputFolderPath.resolve(nodeName);
    return createServer(nomadRoot, nodeName);
  }

  protected NomadServer getNomadServer(String stripeName, String nodeName) throws Exception {
    Path nomadRoot = outputFolderPath.resolve(stripeName + "_" + nodeName);
    return createServer(nomadRoot, nodeName);
  }

  private NomadServer createServer(Path nomadRoot, String nodeName) throws SanskritException, NomadException {
    NomadRepositoryManager nomadRepositoryManager = new NomadRepositoryManager(nomadRoot);
    nomadRepositoryManager.createDirectories();

    ChangeApplicator changeApplicator = new ChangeApplicator() {
      @Override
      public PotentialApplicationResult canApply(final String existing, final NomadChange change) {
        return PotentialApplicationResult.allow(((ConfigMigrationNomadChange) change).getConfiguration());
      }

      @Override
      public void apply(final NomadChange change) {
      }
    };

    return UpgradableNomadServerFactory.createServer(nomadRepositoryManager, changeApplicator, nodeName);
  }

  protected String getUser() {
    return nomadEnvironment.getUser();
  }

  protected String getHost() {
    return nomadEnvironment.getHost();
  }
}