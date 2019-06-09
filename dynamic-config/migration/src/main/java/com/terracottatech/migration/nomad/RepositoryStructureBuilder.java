/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.migration.nomad;

import com.terracottatech.dynamic_config.nomad.ConfigMigrationNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.migration.NodeConfigurationHandler;
import com.terracottatech.migration.exception.ErrorCode;
import com.terracottatech.migration.exception.MigrationException;
import com.terracottatech.migration.util.FileUtility;
import com.terracottatech.migration.util.Pair;
import com.terracottatech.migration.util.XmlUtility;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.NomadServer;
import org.w3c.dom.Node;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;

public class RepositoryStructureBuilder implements NodeConfigurationHandler {
  private final Path outputFolderPath;
  private final UUID nomadRequestId;
  private final NomadEnvironment nomadEnvironment;

  /*
  For integration test only. Need to get reference of NomadServer. IT code will pass the map
  which contains NomadServer for each server. If we have have two servers server-1 and server-2 in a stripe,
  then map will contain entries for server1->NomadServer1 and server2->NomadServer2
  Converted configuration will be read in IT code and will be validated. To read the converted configuration,
  IT will require NomadServer reference.
   */
  //private final Map<String, NomadServer> serverMap;

  public RepositoryStructureBuilder(Path outputFolderPath) {
    this.outputFolderPath = outputFolderPath;
    this.nomadRequestId = UUID.randomUUID();
    this.nomadEnvironment = new NomadEnvironment();
  }

  @Override
  public void process(final Map<Pair<String, String>, Node> nodeNameNodeConfigMap) {
    nodeNameNodeConfigMap.forEach(printToFile());
  }

  protected BiConsumer<Pair<String, String>, Node> printToFile() {
    return (Pair<String, String> stripeNameServerName, Node doc) -> {
      try {
        String xml = getXmlString(doc);
        NomadServer nomadServer = getNomadServer(stripeNameServerName.getOne(), stripeNameServerName.getAnother());
        DiscoverResponse discoverResponse = nomadServer.discover();
        long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
        long nextVersionNumber = discoverResponse.getCurrentVersion() + 1;

        PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), nomadRequestId,
            nextVersionNumber, new ConfigMigrationNomadChange(xml));
        AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
        if (!response.isAccepted()) {
          throw new MigrationException(ErrorCode.UNEXPECTED_ERROR_FROM_NOMAD_PREPARE_PHASE
              , "Response code from nomad:" + response
              .getRejectionReason());
        }
        long nextMutativeMessageCount = mutativeMessageCount + 1;
        CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, getHost(), getUser(), nomadRequestId);
        nomadServer.commit(commitMessage);
      } catch (RuntimeException e) {
        throw e;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    };
  }

  protected NomadServer getNomadServer(String nodeName) throws Exception {
    Path folderForServer = createRootDirectoryForServer(outputFolderPath, nodeName);
    return NomadServerProvider.getNomadServer(folderForServer, nodeName);
  }

  protected NomadServer getNomadServer(String stripeName, String nodeName) throws Exception {
    Path folderForServer = createRootDirectoryForServer(outputFolderPath, stripeName, nodeName);
    return NomadServerProvider.getNomadServer(folderForServer, nodeName);
  }

  protected String getXmlString(Node doc) throws Exception {
    return XmlUtility.getPrettyPrintableXmlString(doc);
  }

  protected Path createRootDirectoryForServer(Path folder, String serverName) throws Exception {
    Path folderForServer = Paths.get(folder + File.separator + serverName);
    FileUtility.createDirectory(folderForServer);
    return folderForServer;
  }

  protected Path createRootDirectoryForServer(Path folder, String stripeName, String serverName) throws Exception {
    Path folderForServer = Paths.get(folder + File.separator + stripeName + "_" + serverName);
    FileUtility.createDirectory(folderForServer);
    return folderForServer;
  }

  protected String getUser() {
    return nomadEnvironment.getUser();
  }

  protected String getHost() {
    return nomadEnvironment.getHost();
  }
}