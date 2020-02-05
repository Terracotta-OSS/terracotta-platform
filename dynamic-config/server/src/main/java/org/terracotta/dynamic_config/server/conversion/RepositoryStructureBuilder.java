/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.conversion;

import org.terracotta.common.struct.Tuple2;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.nomad.ConfigMigrationNomadChange;
import org.terracotta.dynamic_config.api.service.PathResolver;
import org.terracotta.dynamic_config.api.service.XmlConfigMapper;
import org.terracotta.dynamic_config.api.service.XmlConfigMapperDiscovery;
import org.terracotta.dynamic_config.server.conversion.exception.ConfigConversionException;
import org.terracotta.dynamic_config.server.conversion.xml.XmlUtility;
import org.terracotta.dynamic_config.server.nomad.UpgradableNomadServerFactory;
import org.terracotta.dynamic_config.server.nomad.repository.NomadRepositoryManager;
import org.terracotta.dynamic_config.server.service.ParameterSubstitutor;
import org.terracotta.nomad.NomadEnvironment;
import org.terracotta.nomad.client.change.NomadChange;
import org.terracotta.nomad.messages.AcceptRejectResponse;
import org.terracotta.nomad.messages.CommitMessage;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.messages.PrepareMessage;
import org.terracotta.nomad.server.ChangeApplicator;
import org.terracotta.nomad.server.NomadException;
import org.terracotta.nomad.server.NomadServer;
import org.terracotta.nomad.server.PotentialApplicationResult;
import org.terracotta.persistence.sanskrit.SanskritException;
import org.w3c.dom.Node;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.terracotta.dynamic_config.server.conversion.exception.ErrorCode.UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE;

public class RepositoryStructureBuilder {
  private final Path outputFolderPath;
  private final UUID nomadRequestId;
  private final NomadEnvironment nomadEnvironment;
  private final XmlConfigMapper xmlConfigMapper;

  public RepositoryStructureBuilder(Path outputFolderPath) {
    this.xmlConfigMapper = new XmlConfigMapperDiscovery(PathResolver.NOOP).find()
        .orElseThrow(() -> new AssertionError("No " + XmlConfigMapper.class.getName() + " service implementation found on classpath"));
    this.outputFolderPath = outputFolderPath;
    this.nomadRequestId = UUID.randomUUID();
    this.nomadEnvironment = new NomadEnvironment();
  }

  public void process(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap) {
    process(nodeNameNodeConfigMap, false);
  }

  public void process(Map<Tuple2<Integer, String>, Node> nodeNameNodeConfigMap, boolean failForRelativePaths) {
    nodeNameNodeConfigMap.forEach((stripeIdServerName, doc) -> {
      try {
        // create the XML from the manipulated DOM elements
        String xml = XmlUtility.getPrettyPrintableXmlString(doc);
        // convert back the XML to a topology model
        NodeContext nodeContext = xmlConfigMapper.fromXml(stripeIdServerName.t2, xml);
        if (failForRelativePaths) {
          checkRelativePaths(nodeContext);
        }

        // save the topology model into Nomad
        NomadServer<NodeContext> nomadServer = getNomadServer(stripeIdServerName.getT1(), stripeIdServerName.getT2());
        DiscoverResponse<NodeContext> discoverResponse = nomadServer.discover();
        long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
        long nextVersionNumber = discoverResponse.getCurrentVersion() + 1;

        PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), Instant.now(), nomadRequestId,
            nextVersionNumber, new ConfigMigrationNomadChange(nodeContext.getCluster()));
        AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
        if (!response.isAccepted()) {
          throw new ConfigConversionException(UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE, "Response code from nomad:" + response.getRejectionReason());
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

  private void checkRelativePaths(NodeContext nodeContext) {
    org.terracotta.dynamic_config.api.model.Node node = nodeContext.getNode();
    if (node.getDataDirs().values().stream().anyMatch(path -> !path.isAbsolute()) ||
        !node.getNodeBackupDir().isAbsolute() || !node.getNodeLogDir().isAbsolute() || !node.getNodeMetadataDir().isAbsolute() ||
        !node.getSecurityAuditLogDir().isAbsolute() || !node.getSecurityDir().isAbsolute()) {
      throw new RuntimeException("The source config contains relative paths, which will not work as intended after the conversion. Use absolute paths instead.");
    }
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