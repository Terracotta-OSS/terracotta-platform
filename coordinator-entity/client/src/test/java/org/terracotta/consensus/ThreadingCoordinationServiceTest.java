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
import org.terracotta.connection.entity.Entity;
import org.terracotta.consensus.entity.client.CoordinationClientEntity;
import org.terracotta.consensus.entity.messages.Nomination;
import org.terracotta.voltron.proxy.client.messages.MessageListener;
import org.terracotta.voltron.proxy.ClientId;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CyclicBarrier;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Alex Snaps
 */
public class ThreadingCoordinationServiceTest {

  public static final Class<? extends Entity> ENTITY_CLASS = CoordinationClientEntity.class;
  public static final String ENTITY_NAME = "foo";
  public static final int PARTICIPANTS = 3;

  @Test
  public void testRunMultiThreadedSingleNode() throws Throwable {
    final TestCoordinationClientEntity coordinationClientEntity = new TestCoordinationClientEntity(PARTICIPANTS);
    final CoordinationService service = new CoordinationService(coordinationClientEntity);
    final CyclicBarrier barrier = new CyclicBarrier(PARTICIPANTS + 1);
    final ConcurrentMap<Thread, Object> results = new ConcurrentHashMap<Thread, Object>();

    Thread t1 = newElectorThread(service, barrier, results);
    Thread t2 = newElectorThread(service, barrier, results);
    Thread t3 = newElectorThread(service, barrier, results);

    t1.start();
    t2.start();
    t3.start();

    barrier.await();

    t1.join();
    t2.join();
    t3.join();

    assertThat(results.size(), is(1));
  }

  @Test
  public void testRunMultiThreadedMultipleNode() throws Throwable {
    final TestCoordinationClientEntity coordinationClientEntity = new TestCoordinationClientEntity(PARTICIPANTS);
    final CoordinationService service = new CoordinationService(coordinationClientEntity);
    final CyclicBarrier barrier = new CyclicBarrier(PARTICIPANTS + 1);
    final ConcurrentMap<Thread, Object> results = new ConcurrentHashMap<Thread, Object>();
    coordinationClientEntity.elected = new Object();

    Thread t1 = newElectorThread(service, barrier, results);
    Thread t2 = newElectorThread(service, barrier, results);
    Thread t3 = newElectorThread(service, barrier, results);

    t1.start();
    t2.start();
    t3.start();

    barrier.await();
    coordinationClientEntity.accepted = true;
    service.leaderElected(CoordinationService.toString(ENTITY_CLASS, ENTITY_NAME));

    t1.join();
    t2.join();
    t3.join();

    assertThat(results.size(), is(0));
  }

  private Thread newElectorThread(final CoordinationService service, final CyclicBarrier barrier,
                                  final ConcurrentMap<Thread, Object> results) {
    return new Thread() {
      @Override
      public void run() {
        try {
          barrier.await();
          final Object result = service.executeIfLeader(ENTITY_CLASS, ENTITY_NAME, new Callable<Object>() {
            public Object call() throws Exception {
              return new Object();
            }
          });
          if (result != null) {
            results.put(Thread.currentThread(), result);
          }
        } catch (Throwable throwable) {
          results.put(Thread.currentThread(), throwable);
        }
      }
    };
  }

  private static class TestCoordinationClientEntity implements CoordinationClientEntity {

    private int participants;
    private Object elected;
    private boolean accepted = false;

    public TestCoordinationClientEntity(final int participants) {
      this.participants = participants;
    }

    public synchronized Nomination runForElection(final String namespace, @ClientId final Object clientId) {
      if (elected == null) {
        elected = clientId;
        return new Nomination(namespace, 1);
      } else if (!accepted) {
        return new Nomination(namespace);
      }
      return null;
    }

    public synchronized void accept(final String namespace, final Nomination permit) {
      accepted = true;
    }

    public synchronized void delist(final String namespace, @ClientId final Object clientId) {
      throw new UnsupportedOperationException("Implement me!");
    }

    public void close() {
      throw new UnsupportedOperationException("Implement me!");
    }

    public void registerListener(final MessageListener<Nomination> message) {
      // no op
    }
  }
}