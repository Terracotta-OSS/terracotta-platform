/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.nomad;

import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.nomad.ConfigMigrationNomadChange;
import com.terracottatech.dynamic_config.nomad.UpgradableNomadServerFactory;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import com.terracottatech.dynamic_config.util.PathResolver;
import com.terracottatech.dynamic_config.xml.XmlConfigMapper;
import com.terracottatech.migration.exception.MigrationException;
import com.terracottatech.migration.xml.XmlUtility;
import com.terracottatech.nomad.NomadEnvironment;
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
import com.terracottatech.struct.tuple.Tuple2;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static com.terracottatech.migration.exception.ErrorCode.UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE;

public class RepositoryStructureBuilder {
  private final Path outputFolderPath;
  private final UUID nomadRequestId;
  private final NomadEnvironment nomadEnvironment;
  private final XmlConfigMapper xmlConfigMapper;

  public RepositoryStructureBuilder(Path outputFolderPath) {
    this.xmlConfigMapper = new XmlConfigMapper(PathResolver.NOOP);
    this.outputFolderPath = outputFolderPath;
    this.nomadRequestId = UUID.randomUUID();
    this.nomadEnvironment = new NomadEnvironment();
  }

  public void process(final Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap) {
    nodeNameNodeConfigMap.forEach((stripeIdServerName, doc) -> {
      try {
        // create the XML from the manipulated DOM elements
        String xml = XmlUtility.getPrettyPrintableXmlString(doc);
        // convert back the XML to a topology model
        NodeContext nodeContext = xmlConfigMapper.fromXml(stripeIdServerName.t2, xml);
        // save the topology model into Nomad
        NomadServer<NodeContext> nomadServer = getNomadServer(stripeIdServerName.getT1(), stripeIdServerName.getT2());
        DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
        long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
        long nextVersionNumber = discoverResponse.getCurrentVersion() + 1;

        PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), Instant.now(), nomadRequestId,
            nextVersionNumber, new ConfigMigrationNomadChange(nodeContext.getCluster()));
        AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
        if (!response.isAccepted()) {
          throw new MigrationException(UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE, "Response code from nomad:" + response.getRejectionReason());
        }

        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, getHost(), getUser(), Instant.now(), nomadRequestId);
        nomadServer.commit(commitMessage);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    });
  }

  protected NomadServer<NodeContext> getNomadServer(int stripeId, String nodeName) throws Exception {
    Path repositoryPath = outputFolderPath.resolve("stripe" + stripeId + "_" + nodeName);
    return createServer(repositoryPath, stripeId, nodeName);
  }

  private NomadServer<NodeContext> createServer(Path repositoryPath, int stripeId, String nodeName) throws SanskritException, NomadException {
    ParameterSubstitutor parameterSubstitutor = new ParameterSubstitutor();
    NomadRepositoryManager nomadRepositoryManager = new NomadRepositoryManager(repositoryPath, parameterSubstitutor);
    nomadRepositoryManager.createDirectories();

    ChangeApplicator<NodeContext> changeApplicator = new ChangeApplicator<NodeContext>() {
      @Override
      public PotentialApplicationResult<NodeContext> tryApply(final NodeContext existing, final NomadChange change) {
        return PotentialApplicationResult.allow(new NodeContext(
            ((ConfigMigrationNomadChange) change).getCluster(),
            stripeId,
            nodeName
        ));
      }

      @Override
      public void apply(final NomadChange change) {
      }
    };

    return UpgradableNomadServerFactory.createServer(nomadRepositoryManager, changeApplicator, nodeName, parameterSubstitutor);
  }

  protected String getUser() {
    return nomadEnvironment.getUser();
  }

  protected String getHost() {
    return nomadEnvironment.getHost();
  }
}