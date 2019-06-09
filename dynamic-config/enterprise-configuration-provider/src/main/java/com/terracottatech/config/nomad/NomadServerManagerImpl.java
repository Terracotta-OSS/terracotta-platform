/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.config.nomad;

import com.terracottatech.diagnostic.common.Constants;
import com.terracottatech.diagnostic.server.DiagnosticServices;
import com.terracottatech.diagnostic.server.DiagnosticServicesRegistration;
import com.terracottatech.dynamic_config.nomad.ConfigChangeApplicator;
import com.terracottatech.dynamic_config.nomad.ConfigController;
import com.terracottatech.dynamic_config.nomad.ConfigRepairNomadChange;
import com.terracottatech.dynamic_config.nomad.NomadConfigFileNameProvider;
import com.terracottatech.dynamic_config.nomad.NomadEnvironment;
import com.terracottatech.dynamic_config.nomad.NomadJson;
import com.terracottatech.dynamic_config.nomad.SettingNomadChange;
import com.terracottatech.dynamic_config.nomad.SingleThreadedNomadServer;
import com.terracottatech.dynamic_config.nomad.persistence.FileConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.InitialConfigStorage;
import com.terracottatech.dynamic_config.nomad.persistence.SanskritNomadServerState;
import com.terracottatech.dynamic_config.nomad.processor.ApplicabilityNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.RoutingNomadChangeProcessor;
import com.terracottatech.dynamic_config.nomad.processor.SettingNomadChangeProcessor;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.NomadServer;
import com.terracottatech.nomad.server.NomadServerImpl;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import com.terracottatech.persistence.sanskrit.Sanskrit;
import com.terracottatech.persistence.sanskrit.SanskritException;
import com.terracottatech.persistence.sanskrit.file.FileBasedFilesystemDirectory;

import java.nio.file.Path;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class NomadServerManagerImpl implements NomadServerManager {
  private volatile UpgradableNomadServer nomadServer;
  private volatile DiagnosticServicesRegistration<NomadServer> registration;
  private final AtomicReference<STATE> initStateAtomicReference = new AtomicReference<>();
  private final NomadEnvironment nomadEnvironment = new NomadEnvironment();

  public NomadServerManagerImpl() {
    initStateAtomicReference.set(STATE.UNINITIALIZED);
  }

  @Override
  public void init(Path nomadRoot) throws NomadConfigurationException {
    guardInit();
    try {
      NomadRepositoryManager repositoryManager = createNomadRepositoryManager(nomadRoot);
      repositoryManager.createIfAbsent();
      nomadServer = createServer(repositoryManager.getSanskritPath(), repositoryManager.getConfigurationPath());
      registration = DiagnosticServices.register(NomadServer.class, nomadServer);
      registration.registerMBean(Constants.MBEAN_NOMAD);
      initStateAtomicReference.set(STATE.INITIALIZED);
    } catch (Exception e) {
      initStateAtomicReference.set(STATE.INITIALIZATION_FAILED);
      throw new NomadConfigurationException("Exception initializing Nomad Server", e);
    }
  }

  @Override
  public void upgradeForWrite(String nodeName, String stripeName) {
    ConfigController configController = createConfigController(nodeName, stripeName);
    ChangeApplicator changeApplicator = new ConfigChangeApplicator(
        new ApplicabilityNomadChangeProcessor(
            configController,
            new RoutingNomadChangeProcessor().register(SettingNomadChange.class, new SettingNomadChangeProcessor(configController))
        )
    );

    nomadServer.setChangeApplicator(changeApplicator);
  }

  @Override
  public String getConfiguration() throws NomadConfigurationException {
    STATE currentState = initStateAtomicReference.get();
    if (currentState != STATE.INITIALIZED) {
      String errorMessage = "getConfiguration() cannot be invoked when state is " + currentState.name();
      throw new NomadServerManagerStateException(currentState.name(), errorMessage);
    }

    ChangeDetails latestChange;
    try {
      latestChange = nomadServer.discover().getLatestChange();
    } catch (NomadException e) {
      String errorMessage = "Exception while making discover call to Nomad";
      throw new NomadConfigurationException(errorMessage, e);
    }

    String missingConfigErrorMessage = "Did not get last stored configuration from Nomad";
    if (latestChange == null) {
      throw new NomadConfigurationException(missingConfigErrorMessage);
    }

    String configuration = latestChange.getResult();
    if (configuration == null || configuration.isEmpty()) {
      throw new NomadConfigurationException(missingConfigErrorMessage);
    }
    return configuration;
  }

  @Override
  public void repairConfiguration(String configuration, long version) throws NomadConfigurationException {
    try {
      UUID nomadRequestId = UUID.randomUUID();
      DiscoverResponse discoverResponse = nomadServer.discover();
      long mutativeMessageCount = discoverResponse.getMutativeMessageCount();
      PrepareMessage prepareMessage = new PrepareMessage(mutativeMessageCount, getHost(), getUser(), nomadRequestId,
          version, new ConfigRepairNomadChange(configuration));

      AcceptRejectResponse response = nomadServer.prepare(prepareMessage);
      if (!response.isAccepted()) {
        throw new NomadConfigurationException("Repair message is rejected by Nomad. Reason for rejection is "
            + response.getRejectionReason().name());
      }
      long nextMutativeMessageCount = mutativeMessageCount + 1;
      CommitMessage commitMessage = new CommitMessage(nextMutativeMessageCount, getHost(), getUser(), nomadRequestId);
      AcceptRejectResponse commitResponse = nomadServer.commit(commitMessage);
      if (!commitResponse.isAccepted()) {
        throw new NomadConfigurationException("Unexpected commit failure. Reason for failure is "
            + commitResponse.getRejectionReason().name());
      }
    } catch (NomadException e) {
      throw new NomadConfigurationException("Unable to repair configuration", e);
    }
  }

  @Override
  public void close() {
    if (registration != null) {
      registration.close();
      registration = null;
    }
  }

  UpgradableNomadServer createServer(Path sanskritPath, Path configPath) throws SanskritException, NomadException {
    return new SingleThreadedNomadServer(
        new NomadServerImpl(
            new SanskritNomadServerState(
                Sanskrit.init(
                    new FileBasedFilesystemDirectory(sanskritPath),
                    NomadJson.buildObjectMapper()
                ),
                new InitialConfigStorage(
                    new FileConfigStorage(
                        configPath,
                        NomadConfigFileNameProvider.getFileNameProvider(configPath, null)
                    )
                )
            ),
            null
        )
    );
  }

  void guardInit() throws NomadConfigurationException {
    STATE currentInitState = initStateAtomicReference.get();
    if (currentInitState != STATE.UNINITIALIZED) {
      throw new NomadConfigurationException("NomadServerManager is not in UNINITIALIZED state");
    }
    if (!initStateAtomicReference.compareAndSet(STATE.UNINITIALIZED, STATE.INITIALIZING)) {
      throw new NomadConfigurationException("NomadServerManager is getting initialized concurrently");
    }
  }

  NomadRepositoryManager createNomadRepositoryManager(Path nomadRoot) {
    return new NomadRepositoryManager(nomadRoot);
  }

  ConfigController createConfigController(String nodeName, String stripeName) {
    return new ConfigControllerImpl(() -> nodeName, () -> stripeName);
  }

  String getHost() {
    return nomadEnvironment.getHost();
  }

  String getUser() {
    return nomadEnvironment.getUser();
  }

  enum STATE {
    UNINITIALIZED,
    INITIALIZING,
    INITIALIZED,
    INITIALIZATION_FAILED
  }
}
