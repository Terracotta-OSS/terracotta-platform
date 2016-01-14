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

import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.Entity;
import org.terracotta.connection.entity.EntityRef;
import org.terracotta.consensus.entity.ElectionResponse;
import org.terracotta.consensus.entity.LeaderOffer;
import org.terracotta.consensus.entity.Versions;
import org.terracotta.consensus.entity.client.CoordinationClientEntity;
import org.terracotta.consensus.entity.messages.ServerElectionEvent;
import org.terracotta.exception.EntityAlreadyExistsException;
import org.terracotta.exception.EntityException;
import org.terracotta.exception.EntityNotFoundException;
import org.terracotta.voltron.proxy.client.messages.MessageListener;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

/**
 * Convenience service to deal with Leader election and executing code as such
 * This implementation will use a server-side entity that is to never be deleted
 *
 * @author Alex Snaps
 */
public class CoordinationService {

  static final String SINGLETON_NAME = CoordinationService.class.getName() + "::__oneToRuleThemAll";

  private final CoordinationClientEntity entity;

  private ConcurrentMap<String, Object> syncs = new ConcurrentHashMap<String, Object>();

  /**
   * Constructor that will also try to create the cluster wide entity should it not yet be present
   *
   * @param connection the connection to the stripe where the entity is to exist
   * @throws AssertionError should the just created entity be deleted when retrieved back
   */
  public CoordinationService(Connection connection) throws AssertionError {
    this(getCoordinationClientEntity(connection));
  }

  CoordinationService(CoordinationClientEntity coordinationClientEntity) {
    entity = coordinationClientEntity;
    entity.registerListener(new MessageListener<ServerElectionEvent<String>>() {
      public void onMessage(final ServerElectionEvent<String> message) {
        switch (message.getType()) {
          case CHANGED: electionChanged(message.getNamespace());
          case COMPLETED: electionCompleted(message.getNamespace());
        }
      }
    });
  }

  /**
   * Will enlist for election for the {@code entityType}/{@code entityName} pair.
   * Should the election be won, the {@link Callable} passed in will be invoked. The returned value is then returned to
   * the user. Since {@code null} is returned when the election isn't won, best is to not return {@code null} from
   * the {@link Callable} neither, as the user wouldn't be able to tell whether is now leader for the given
   * {@code entityType}/{@code entityName} pair.
   *
   * @param entityType the type of the entity we're running for election for, can't be null
   * @param entityName the name of the entity we're running for election for, can't be null
   * @param callable the callable to invoke, should we win, can't be null
   * @param <T> the value to return
   * @return the callable returned {@code T} should the election be won, {@code null} otherwise
   * @throws Throwable whatever the {@code callable} may throw
   */
  public <T> T executeIfLeader(Class<? extends Entity> entityType, String entityName, ElectionTask<T> callable) throws ExecutionException {

    if(entityType == null || entityName == null || callable == null) {
      throw new NullPointerException();
    }

    final String namespace = toString(entityType, entityName);

    Object sync = new Object();
    synchronized (sync) {
      Object actualSync = syncs.putIfAbsent(namespace, sync);
      if (actualSync == null) {
        actualSync = sync;
      }
      synchronized (actualSync) {
        boolean interrupted = false;
        ElectionResponse response;
        try {
          while ((response = entity.runForElection(namespace, Thread.currentThread())).isPending()) {
            try {
              actualSync.wait();
            } catch (InterruptedException ex) {
              interrupted = true;
            }
          }
        } finally {
          if (interrupted) {
            Thread.currentThread().interrupt();
          }
        }
        if (response instanceof LeaderOffer) {
          LeaderOffer offer = (LeaderOffer) response;
          boolean accepted = false;
          try {
            final T t;
            try {
              t = callable.call(offer.clean());
            } catch (Throwable e) {
              throw new ExecutionException(e);
            }
            entity.accept(namespace, offer);
            accepted = true;
            return t;
          } finally {
            if (!accepted) {
              entity.delist(namespace, this);
            }
          }
        }
      }
    }
    return null;
  }

  void electionCompleted(String namespace) {
    Object actualSync = syncs.get(namespace);
    if (actualSync != null) {
      synchronized (actualSync) {
        try {
          if (!syncs.remove(namespace, actualSync)) {
            throw new AssertionError("Broken FSM!");
          }
        } finally {
          actualSync.notifyAll();
        }
      }
    }
  }
  
  void electionChanged(String namespace) {
    Object actualSync = syncs.get(namespace);
    if (actualSync != null) {
      synchronized (actualSync) {
        actualSync.notifyAll();
      }
    }
  }

  static String toString(final Class<? extends Entity> entityType, final String entityName) {
    return entityType.getName() + "::" + entityName;
  }

  /**
   * Deregisters interest in being/becoming leader for the given {@code entityType}/{@code entityName} pair.
   * @param entityType
   * @param name
   */
  public void delist(Class<? extends Entity> entityType, String name) {
    entity.delist(toString(entityType, name), this);
  }

  /**
   * Closes the entity
   */
  public void close() {
    entity.close();
  }

  private static CoordinationClientEntity getCoordinationClientEntity(final Connection connection) {
    CoordinationClientEntity entity;
    try {
      final EntityRef<CoordinationClientEntity, Object> entityRef
          = connection.getEntityRef(CoordinationClientEntity.class, Versions.LATEST.version(), SINGLETON_NAME);
      try {
        entity = entityRef.fetchEntity();
      } catch (EntityNotFoundException e) {
        try {
          entityRef.create(null);
        } catch (EntityAlreadyExistsException weLostTheRace) {
          // Ignore, that's fine!
        }
        entity = entityRef.fetchEntity();
      }
    } catch (EntityException e) {
      throw new IllegalStateException("Something is definitively wrong with the setup here!", e);
    }
    return entity;
  }

  public interface ElectionTask<T> {

    T call(boolean clean) throws Exception;
  }
}
