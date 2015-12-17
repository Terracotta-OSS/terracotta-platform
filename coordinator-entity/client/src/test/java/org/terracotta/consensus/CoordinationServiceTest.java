/*
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at
 *
 * http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Connection API.
 *
 * The Initial Developer of the Covered Software is
 * Terracotta, Inc., a Software AG company
 */

package org.terracotta.consensus;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.consensus.entity.ElectionResponse;
import org.terracotta.consensus.entity.Versions;
import org.terracotta.consensus.entity.client.CoordinationClientEntity;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;

import java.util.concurrent.Callable;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import org.terracotta.consensus.entity.LeaderOffer;

/**
 * @author Alex Snaps
 */
public class CoordinationServiceTest {

  public static final Class<CoordinationClientEntity> ENTITY_TYPE = CoordinationClientEntity.class;
  public static final String ENTITY_NAME = "name";
  public static final String FLAT_NAME = CoordinationService.toString(ENTITY_TYPE, ENTITY_NAME);

  final long version = Versions.LATEST.version();

  @Test
  public void createsSingletonInstanceIfNonExistent() throws EntityException {
    final Connection connection = mock(Connection.class);
    final EntityRef entityRef = mock(EntityRef.class);
    when(connection.getEntityRef(CoordinationClientEntity.class, version, CoordinationService.SINGLETON_NAME)).thenReturn(entityRef);
    when(entityRef.fetchEntity()).thenThrow(new EntityNotFoundException(ENTITY_TYPE.getName(), ENTITY_NAME)).thenReturn(mock(CoordinationClientEntity.class));
    new CoordinationService(connection);
    verify(entityRef, atLeastOnce()).create(null);
    verify(connection, times(1)).getEntityRef(CoordinationClientEntity.class, version, CoordinationService.SINGLETON_NAME);
  }

  @Test
  public void usesExistingEntityIfPresent() throws EntityException {
    final Connection connection = mockInitialConnection(mock(CoordinationClientEntity.class));
    new CoordinationService(connection);
    verify(connection, times(1)).getEntityRef(CoordinationClientEntity.class, version, CoordinationService.SINGLETON_NAME);
  }

  @Test
  public void throwsWhenFailingToRetrieveAfterCreate() throws EntityException {
    final Connection connection = mock(Connection.class);
    final EntityRef entityRef = mock(EntityRef.class);
    when(connection.getEntityRef(CoordinationClientEntity.class, 1L, CoordinationService.SINGLETON_NAME)).thenReturn(entityRef);
    when(entityRef.fetchEntity()).thenThrow(new EntityNotFoundException(ENTITY_TYPE.getName(), ENTITY_NAME));
    try {
      new CoordinationService(connection);
      fail("this should have thrown!");
    } catch (IllegalStateException e) {
      // expected
    }
    verify(entityRef, atLeastOnce()).create(null);
    verify(connection, times(1)).getEntityRef(CoordinationClientEntity.class, version, CoordinationService.SINGLETON_NAME);
  }

  @Test
  public void doesInvokeCallableWhenElectionWonAndAcceptsNomination() throws Throwable {
    final CoordinationClientEntity CoordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(CoordinationClientEntity));
    final Callable callable = mock(Callable.class);
    final ElectionResponse response = mock(LeaderOffer.class);
    when(CoordinationClientEntity.runForElection(eq(FLAT_NAME), anyObject())).thenReturn(response);
    coordinationService.executeIfLeader(ENTITY_TYPE, ENTITY_NAME, callable);

    InOrder inOrder = Mockito.inOrder(callable, response);

    inOrder.verify(callable, times(1)).call();
//    inOrder.verify(nomination, times(1)).accept();
  }

  @Test
  public void doesInvokeCallableWhenElectionWonAndDeclinesNominationOnCallableThrowing() throws Throwable {
    final CoordinationClientEntity CoordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(CoordinationClientEntity));
    final Callable callable = mock(Callable.class);
    final AssertionError assertionError = new AssertionError();
    when(callable.call()).thenThrow(assertionError);
    final ElectionResponse nomination = mock(LeaderOffer.class);
    when(CoordinationClientEntity.runForElection(eq(FLAT_NAME), anyObject())).thenReturn(nomination);

    try {
      coordinationService.executeIfLeader(ENTITY_TYPE, ENTITY_NAME, callable);
      fail("this should have thrown");
    } catch (AssertionError throwable) {
      assertSame(throwable, assertionError);
    }

    InOrder inOrder = Mockito.inOrder(callable, nomination);

    inOrder.verify(callable, times(1)).call();
//    inOrder.verify(nomination, times(1)).decline();
  }

  @Test
  public void returnsCallableReturnValue() throws Throwable {
    final CoordinationClientEntity CoordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(CoordinationClientEntity));
    final Callable callable = mock(Callable.class);
    final Object o = new Object();
    when(callable.call()).thenReturn(o);
    final ElectionResponse response = mock(LeaderOffer.class);
    when(CoordinationClientEntity.runForElection(eq(FLAT_NAME), anyObject())).thenReturn(response);
    assertSame(o, coordinationService.executeIfLeader(ENTITY_TYPE, ENTITY_NAME, callable));
  }

  @Test
  public void doesNotAcceptNullCallableAndDoesNotEnlistWhenNull() throws Throwable {
    final CoordinationClientEntity coordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(coordinationClientEntity));
    reset(coordinationClientEntity);
    try {
      coordinationService.executeIfLeader(ENTITY_TYPE, ENTITY_NAME, null);
      fail("this should have thrown");
    } catch (NullPointerException e) {
      // expected
    }
    verifyNoMoreInteractions(coordinationClientEntity);
  }

  @Test
  public void doesNotAcceptNullEntityTypeAndDoesNotEnlistWhenNull() throws Throwable {
    final CoordinationClientEntity coordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(coordinationClientEntity));
    reset(coordinationClientEntity);
    try {
      coordinationService.executeIfLeader(null, ENTITY_NAME, mock(Callable.class));
      fail("this should have thrown");
    } catch (NullPointerException e) {
      // expected
    }
    verifyNoMoreInteractions(coordinationClientEntity);
  }

  @Test
  public void doesNotAcceptNullEntityNameAndDoesNotEnlistWhenNull() throws Throwable {
    final CoordinationClientEntity coordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(coordinationClientEntity));
    reset(coordinationClientEntity);
    try {
      coordinationService.executeIfLeader(ENTITY_TYPE, null, mock(Callable.class));
      fail("this should have thrown");
    } catch (NullPointerException e) {
      // expected
    }
    verifyNoMoreInteractions(coordinationClientEntity);
  }

  @Test
  public void delistDelegatesToEntity() throws Throwable {
    final CoordinationClientEntity CoordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(CoordinationClientEntity));
    coordinationService.delist(ENTITY_TYPE, ENTITY_NAME);
    verify(CoordinationClientEntity, times(1)).delist(eq(FLAT_NAME), anyObject());
  }

  @Test
  public void closeDelegatesToEntity() throws Throwable {
    final CoordinationClientEntity CoordinationClientEntity = mock(CoordinationClientEntity.class);
    CoordinationService coordinationService = new CoordinationService(mockInitialConnection(CoordinationClientEntity));
    coordinationService.close();
    verify(CoordinationClientEntity, times(1)).close();
  }

  private Connection mockInitialConnection(final CoordinationClientEntity CoordinationClientEntity) throws EntityException {
    final Connection connection = mock(Connection.class);
    final EntityRef entityRef = mock(EntityRef.class);
    when(connection.getEntityRef(CoordinationClientEntity.class, version, CoordinationService.SINGLETON_NAME)).thenReturn(entityRef);
    when(entityRef.fetchEntity()).thenReturn(CoordinationClientEntity);
    return connection;
  }

}