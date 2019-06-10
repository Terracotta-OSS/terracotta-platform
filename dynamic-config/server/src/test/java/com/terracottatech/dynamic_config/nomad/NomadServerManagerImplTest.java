/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.nomad;

import com.terracottatech.dynamic_config.nomad.exception.NomadConfigurationException;
import com.terracottatech.dynamic_config.repository.MalformedRepositoryException;
import com.terracottatech.dynamic_config.repository.NomadRepositoryManager;
import com.terracottatech.nomad.messages.AcceptRejectResponse;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.CommitMessage;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.messages.PrepareMessage;
import com.terracottatech.nomad.messages.RejectionReason;
import com.terracottatech.nomad.server.ChangeApplicator;
import com.terracottatech.nomad.server.NomadException;
import com.terracottatech.nomad.server.UpgradableNomadServer;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import static com.terracottatech.utilities.hamcrest.ExceptionMatcher.throwing;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NomadServerManagerImplTest {

  @Spy
  NomadServerManagerImpl spyNomadManager;

  @After
  public void tearDown() {
    spyNomadManager.close();
  }

  @Test
  public void testCreateConfigController() {
    ConfigController configController = spyNomadManager.createConfigController("node0", "stripe1");
    assertThat(configController.getNodeName(), is("node0"));
    assertThat(configController.getStripeName(), is("stripe1"));
  }

  @Test
  public void testInitFail() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    Exception exception = mock(NomadException.class);
    doThrow(exception).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    try {
      spyNomadManager.init(nomadRoot);
      fail("Expected NomadConfigurationException");
    } catch (NomadConfigurationException e) {
      assertThat(e.getCause(), is(notNullValue()));
      AtomicReference<NomadServerManagerImpl.STATE> serverStateRef = getServerState(spyNomadManager);
      NomadServerManagerImpl.STATE state = serverStateRef.get();
      assertThat(state, is(NomadServerManagerImpl.STATE.INITIALIZATION_FAILED));
    }
  }

  @Test
  public void testInitFailWithNomadConfigurationException() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    MalformedRepositoryException exception = new MalformedRepositoryException("Failed to create repository");
    doThrow(exception).when(repositoryStructureManager).createIfAbsent();
    //Reset
    resetServerState(spyNomadManager);
    //Init
    try {
      spyNomadManager.init(nomadRoot);
      fail("Expected NomadConfigurationException");
    } catch (NomadConfigurationException e) {
      assertThat(e.getCause(), is(exception));
      AtomicReference<NomadServerManagerImpl.STATE> serverStateRef = getServerState(spyNomadManager);
      NomadServerManagerImpl.STATE state = serverStateRef.get();
      assertThat(state, is(NomadServerManagerImpl.STATE.INITIALIZATION_FAILED));
    }
  }

  @Test
  public void testInitUpgradeAndDestroy() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    verify(spyNomadManager, times(1)).createServer(repositoryStructureManager);

    String nodeName = "node0";
    String stripeName = "stripe1";
    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(nodeName, stripeName);
    verify(upgradableNomadServer, times(1)).setChangeApplicator(any(ChangeApplicator.class));

    //Destroy
    spyNomadManager.close();
  }

  @Test
  public void testGetConfigurationWithExceptionInDiscover() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    NomadException exception = mock(NomadException.class);
    doThrow(exception).when(upgradableNomadServer).discover();

    assertThat(() -> spyNomadManager.getConfiguration(), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testGetConfigurationWithEmptyLatestChange() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();
    when(response.getLatestChange()).thenReturn(null);

    assertThat(() -> spyNomadManager.getConfiguration(), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testGetConfigurationWithNullConfigurationString() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();
    ChangeDetails changeDetails = mock(ChangeDetails.class);
    when(response.getLatestChange()).thenReturn(changeDetails);

    when(changeDetails.getResult()).thenReturn(null);

    assertThat(() -> spyNomadManager.getConfiguration(), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testGetConfigurationWithEmptyConfigurationString() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();
    ChangeDetails changeDetails = mock(ChangeDetails.class);
    when(response.getLatestChange()).thenReturn(changeDetails);
    when(changeDetails.getResult()).thenReturn("");

    assertThat(() -> spyNomadManager.getConfiguration(), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testGetConfiguration() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();
    ChangeDetails changeDetails = mock(ChangeDetails.class);
    when(response.getLatestChange()).thenReturn(changeDetails);
    when(changeDetails.getResult()).thenReturn("Hello");

    String returnedConfiguration = spyNomadManager.getConfiguration();
    assertThat(returnedConfiguration, is("Hello"));
  }

  @Test
  public void testGetConfigurationWithUninitializedServer() throws Exception {
    //Reset
    resetServerState(spyNomadManager);

    assertThat(() -> spyNomadManager.getConfiguration(), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testRepairConfiguration() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    String nodeName = "node0";
    String stripeName = "stripe1";
    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(nodeName, stripeName);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();

    String newConfiguration = "Blahhhh";
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    AcceptRejectResponse prepareResponse = mock(AcceptRejectResponse.class);
    when(prepareResponse.isAccepted()).thenReturn(true);

    AcceptRejectResponse commitResponse = mock(AcceptRejectResponse.class);
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
  public void testRepairConfigurationWithPrepareFail() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    String nodeName = "node0";
    String stripeName = "stripe1";
    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(nodeName, stripeName);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();

    String newConfiguration = "Blahhhh";
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    AcceptRejectResponse prepareResponse = mock(AcceptRejectResponse.class);
    when(prepareResponse.isAccepted()).thenReturn(false);
    when(prepareResponse.getRejectionReason()).thenReturn(RejectionReason.BAD);

    doReturn(prepareResponse).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    assertThat(() -> spyNomadManager.repairConfiguration(newConfiguration, version), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testRepairConfigurationWithServerThrowingNomadException() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    String nodeName = "node0";
    String stripeName = "stripe1";
    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(nodeName, stripeName);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();

    String newConfiguration = "Blahhhh";
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    NomadException prepareException = mock(NomadException.class);

    doThrow(prepareException).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    assertThat(() -> spyNomadManager.repairConfiguration(newConfiguration, version), is(throwing(instanceOf(NomadConfigurationException.class))));
  }

  @Test
  public void testRepairConfigurationWithCommitFail() throws Exception {
    Path nomadRoot = mock(Path.class);
    NomadRepositoryManager repositoryStructureManager = mock(NomadRepositoryManager.class);
    doReturn(repositoryStructureManager).when(spyNomadManager).createNomadRepositoryManager(nomadRoot);
    UpgradableNomadServer upgradableNomadServer = mock(UpgradableNomadServer.class);
    doReturn(upgradableNomadServer).when(spyNomadManager).createServer(repositoryStructureManager);

    //Reset
    resetServerState(spyNomadManager);
    //Init
    spyNomadManager.init(nomadRoot);

    String nodeName = "node0";
    String stripeName = "stripe1";
    doNothing().when(upgradableNomadServer).setChangeApplicator(any(ChangeApplicator.class));

    //Upgrade
    spyNomadManager.upgradeForWrite(nodeName, stripeName);

    doReturn("user").when(spyNomadManager).getUser();
    doReturn("127.0.0.1").when(spyNomadManager).getHost();

    DiscoverResponse response = mock(DiscoverResponse.class);
    doReturn(response).when(upgradableNomadServer).discover();

    String newConfiguration = "Blahhhh";
    long version = 10L;

    when(response.getMutativeMessageCount()).thenReturn(5L);
    AcceptRejectResponse prepareResponse = mock(AcceptRejectResponse.class);
    when(prepareResponse.isAccepted()).thenReturn(true);

    AcceptRejectResponse commitResponse = mock(AcceptRejectResponse.class);
    when(commitResponse.isAccepted()).thenReturn(false);
    when(commitResponse.getRejectionReason()).thenReturn(RejectionReason.BAD);

    doReturn(prepareResponse).when(upgradableNomadServer).prepare(any(PrepareMessage.class));
    doReturn(commitResponse).when(upgradableNomadServer).commit(any(CommitMessage.class));

    ArgumentCaptor<PrepareMessage> argumentCaptorPrepare = ArgumentCaptor.forClass(PrepareMessage.class);
    ArgumentCaptor<CommitMessage> argumentCaptorCommit = ArgumentCaptor.forClass(CommitMessage.class);

    assertThat(
        () -> spyNomadManager.repairConfiguration(newConfiguration, version),
        is(throwing(instanceOf(NomadConfigurationException.class)).andMessage(is(equalTo("Unexpected commit failure. Reason for failure is BAD")))));

    verify(upgradableNomadServer).prepare(argumentCaptorPrepare.capture());
    verify(upgradableNomadServer).commit(argumentCaptorCommit.capture());

    PrepareMessage prepareMessage = argumentCaptorPrepare.getValue();
    assertThat(prepareMessage.getVersionNumber(), is(10L));

    CommitMessage commitMessage = argumentCaptorCommit.getValue();
    assertThat(commitMessage.getExpectedMutativeMessageCount(), is(6L));
  }

  @Test
  public void testGuardInit() throws Exception {
    //Reset
    resetServerState(spyNomadManager);
    spyNomadManager.guardInit();
    try {
      spyNomadManager.guardInit();
      fail("Should get NomadConfigurationException");
    } catch (NomadConfigurationException e) {

    }
  }

  private AtomicReference<NomadServerManagerImpl.STATE> getServerState(NomadServerManagerImpl nomadManager) throws Exception {
    Field field = NomadServerManagerImpl.class.getDeclaredField("initStateAtomicReference");
    field.setAccessible(true);
    Field modifiersField = Field.class.getDeclaredField("modifiers");
    modifiersField.setAccessible(true);
    modifiersField.setInt(field, field.getModifiers() & ~Modifier.FINAL);
    @SuppressWarnings("unchecked")
    AtomicReference<NomadServerManagerImpl.STATE> initStateAtomicReference =
        (AtomicReference<NomadServerManagerImpl.STATE>) field.get(nomadManager);
    return initStateAtomicReference;
  }

  private void resetServerState(NomadServerManagerImpl nomadManager) throws Exception {
    AtomicReference<NomadServerManagerImpl.STATE> initStateAtomicReference =
        getServerState(nomadManager);
    initStateAtomicReference.set(NomadServerManagerImpl.STATE.UNINITIALIZED);
  }
}
