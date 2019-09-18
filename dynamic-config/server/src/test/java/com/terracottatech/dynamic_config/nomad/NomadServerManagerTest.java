/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.model.Cluster;
import com.terracottatech.dynamic_config.model.Node;
import com.terracottatech.dynamic_config.model.NodeContext;
import com.terracottatech.dynamic_config.model.Stripe;
import com.terracottatech.dynamic_config.model.exception.MalformedRepositoryException;
import com.terracottatech.dynamic_config.nomad.NomadBootstrapper.NomadServerManager;
import com.terracottatech.dynamic_config.nomad.exception.NomadConfigurationException;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.dynamic_config.util.IParameterSubstitutor;
import com.terracottatech.dynamic_config.util.ParameterSubstitutor;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.file.Path;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadServerManagerTest {
  private static final NodeContext TOPOLOGY = new NodeContext(new Cluster("foo", new Stripe(new Node().setNodeName("bar"))), 1, "bar");
  private static final IParameterSubstitutor PARAMETER_SUBSTITUTOR = new ParameterSubstitutor();
  private static final String NODE_NAME = "node0";
  private static final int STRIPE_ID = 1;

  private Path repositoryPath;
  private NomadServerManager spyNomadManager;
  private DiscoverResponse<NodeContext> response;
  private ChangeDetails<NodeContext> changeDetails;
  private NomadRepositoryManager repositoryStructureManager;
  private UpgradableNomadServer<NodeContext> upgradableNomadServer;
  private AcceptRejectResponse prepareResponse;
  private AcceptRejectResponse commitResponse;

  Cluster newConfiguration = new Cluster(new Stripe(new Node().setNodeName(NODE_NAME)));

  @Before
  @SuppressWarnings("unchecked")
  public void setUp() {
    repositoryPath = mock(Path.class);
    response = mock(DiscoverResponse.class);
    repositoryStructureManager = mock(NomadRepositoryManager.class);
    spyNomadManager = spy(NomadServerManager.class);
    upgradableNomadServer = mock(UpgradableNomadServer.class);
    changeDetails = mock(ChangeDetails.class);
    prepareResponse = mock(AcceptRejectResponse.class);
    commitResponse = mock(AcceptRejectResponse.class);
  }

  @Test
  public void testInitFail() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doThrow(NomadException.class).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    assertThat(
        () -> spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Exception initializing Nomad Server")))
    );
  }

  @Test
  public void testInitFailWithNomadConfigurationException() {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doThrow(MalformedRepositoryException.class).when(repositoryStructureManager).createDirectories();

    assertThat(
        () -> spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Exception initializing Nomad Server")))
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInitUpgradeAndDestroy() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    verify(spyNomadManager, times(STRIPE_ID)).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));
    spyNomadManager.upgradeForWrite(STRIPE_ID, NODE_NAME, newConfiguration);

    verify(upgradableNomadServer, times(STRIPE_ID)).setChangeApplicator(any(ChangeApplicator.class));
  }

  @Test
  public void testGetConfigurationWithExceptionInDiscover() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doThrow(NomadException.class).when(upgradableNomadServer).discover();

    assertThat(
        spyNomadManager::getConfiguration,
        is(throwing(instanceOf(NomadConfigurationException.class)))
    );
  }

  @Test
  public void testGetConfigurationWithEmptyLatestChange() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doReturn(response).when(upgradableNomadServer).discover();
    when(response.getLatestChange()).thenReturn(null);

    assertThat(
        spyNomadManager::getConfiguration,
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Did not get last stored configuration from Nomad")))
    );
  }

  @Test
  public void testGetConfigurationWithNullConfigurationString() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doReturn(response).when(upgradableNomadServer).discover();
    when(response.getLatestChange()).thenReturn(changeDetails);
    when(changeDetails.getResult()).thenReturn(null);

    assertThat(
        spyNomadManager::getConfiguration,
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Did not get last stored configuration from Nomad")))
    );
  }

  @Test
  public void testGetConfiguration() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doReturn(response).when(upgradableNomadServer).discover();
    when(response.getLatestChange()).thenReturn(changeDetails);
    when(changeDetails.getResult()).thenReturn(TOPOLOGY);

    NodeContext returnedConfiguration = spyNomadManager.getConfiguration();
    assertThat(returnedConfiguration, is(TOPOLOGY));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRepairConfiguration() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);
    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(STRIPE_ID, NODE_NAME, newConfiguration);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    doReturn(response).when(upgradableNomadServer).discover();


    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    when(prepareResponse.isAccepted()).thenReturn(true);

    when(commitResponse.isAccepted()).thenReturn(true);

    doReturn(prepareResponse).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    doReturn(commitResponse).when(upgradableNomadServer).commit(any(CommitMessage.class));

    ArgumentCaptor<PrepareMessage> argumentCaptorPrepare = ArgumentCaptor.forClass(PrepareMessage.class);
    ArgumentCaptor<CommitMessage> argumentCaptorCommit = ArgumentCaptor.forClass(CommitMessage.class);

    spyNomadManager.repairConfiguration(newConfiguration, version);

    verify(upgradableNomadServer).prepare(argumentCaptorPrepare.capture());
    verify(upgradableNomadServer).commit(argumentCaptorCommit.capture());

    PrepareMessage prepareMessage = argumentCaptorPrepare.getValue();
    assertThat(prepareMessage.getVersionNumber(), is(10L));

    CommitMessage commitMessage = argumentCaptorCommit.getValue();
    assertThat(commitMessage.getExpectedMutativeMessageCount(), is(6L));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRepairConfigurationWithPrepareFail() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(STRIPE_ID, NODE_NAME, newConfiguration);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    doReturn(response).when(upgradableNomadServer).discover();

    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    when(prepareResponse.isAccepted()).thenReturn(false);
    when(prepareResponse.getRejectionReason()).thenReturn(RejectionReason.BAD);

    doReturn(prepareResponse).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    assertThat(
        () -> spyNomadManager.repairConfiguration(newConfiguration, version),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Repair message is rejected by Nomad. Reason for rejection is BAD")))
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRepairConfigurationWithServerThrowingNomadException() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(spyNomadManager).registerDiagnosticService();
    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(STRIPE_ID, NODE_NAME, newConfiguration);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    doReturn(response).when(upgradableNomadServer).discover();

    Cluster newConfiguration = new Cluster(new Stripe(new Node().setNodeName("foo")));
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    doThrow(NomadException.class).when(upgradableNomadServer).prepare(any(PrepareMessage.class));

    assertThat(
        () -> spyNomadManager.repairConfiguration(newConfiguration, version),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(containsString("Unable to repair configuration")))
    );
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testRepairConfigurationWithCommitFail() throws Exception {
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(repositoryPath, PARAMETER_SUBSTITUTOR);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager, NODE_NAME, PARAMETER_SUBSTITUTOR);

    spyNomadManager.init(repositoryPath, NODE_NAME, PARAMETER_SUBSTITUTOR);

    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(STRIPE_ID, NODE_NAME, newConfiguration);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    doReturn(response).when(upgradableNomadServer).discover();

    Cluster newConfiguration = new Cluster(new Stripe(new Node().setNodeName("foo")));
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    when(prepareResponse.isAccepted()).thenReturn(true);
    when(commitResponse.isAccepted()).thenReturn(false);
    when(commitResponse.getRejectionReason()).thenReturn(RejectionReason.BAD);

    doReturn(prepareResponse).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    doReturn(commitResponse).when(upgradableNomadServer).commit(any(CommitMessage.class));

    ArgumentCaptor<PrepareMessage> argumentCaptorPrepare = ArgumentCaptor.forClass(PrepareMessage.class);
    ArgumentCaptor<CommitMessage> argumentCaptorCommit = ArgumentCaptor.forClass(CommitMessage.class);

    assertThat(
        () -> spyNomadManager.repairConfiguration(newConfiguration, version),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(is(equalTo("Unexpected commit failure. Reason for failure is BAD"))))
    );

    verify(upgradableNomadServer).prepare(argumentCaptorPrepare.capture());
    verify(upgradableNomadServer).commit(argumentCaptorCommit.capture());

    PrepareMessage prepareMessage = argumentCaptorPrepare.getValue();
    assertThat(prepareMessage.getVersionNumber(), is(10L));

    CommitMessage commitMessage = argumentCaptorCommit.getValue();
    assertThat(commitMessage.getExpectedMutativeMessageCount(), is(6L));
  }
}
