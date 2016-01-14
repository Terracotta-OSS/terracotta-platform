/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.consensus.CoordinationService.ElectionTask;
import org.terracotta.passthrough.PassthroughServer;

import static org.hamcrest.core.Is.is;
import org.hamcrest.core.IsInstanceOf;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.terracotta.consensus.TestUtils.createServer;
import static org.terracotta.consensus.TestUtils.inOtherThread;

/**
 *
 * @author cdennis
 */
public class DoubleParticipantTest {

  @Test
  public void testLosesAlreadyWonElection() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService serviceA = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
          return 1;
        }
      }), is(1));
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            assertTrue(clean);
            throw new AssertionError();
          }
        }), nullValue());
      } finally {
        serviceB.close();
      }
    } finally {
      serviceA.close();
    }
  }
  
  @Test
  public void testWinsAbandonedLeadership() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService serviceA = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
          return 1;
        }
      }), is(1));
      serviceA.delist(Entity.class, "foo");
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            assertTrue(clean);
            return 2;
          }
        }), is(2));
      } finally {
        serviceB.close();
      }
    } finally {
      serviceA.close();
    }
  }
  
  @Test
  public void testTakesOverAbandonedLeadership() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService serviceA = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
          return 1;
        }
      }), is(1));
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            throw new AssertionError();
          }
        }), nullValue());
        
        serviceA.delist(Entity.class, "foo");
        
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            assertTrue(clean);
            return 2;
          }
        }), is(2));
      } finally {
        serviceB.close();
      }
    } finally {
      serviceA.close();
    }
  }

  @Test
  public void testWinsAfterAbandonedElectionOffer() throws Throwable {
    final PassthroughServer server = createServer();

    final CyclicBarrier barrier = new CyclicBarrier(2);
    
    CoordinationService serviceA = new CoordinationService(server.connectNewClient());
    try {
      Future<Integer> other = inOtherThread(new Callable<Integer>() {
        public Integer call() throws Exception {
          CoordinationService serviceB = new CoordinationService(server.connectNewClient());
          try {
            barrier.await();
            return serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
              public Integer call(boolean clean) {
                assertFalse(clean);
                return 1;
              }
            });
          } catch (Throwable e) {
            throw new AssertionError(e);
          } finally {
            serviceB.close();
          }
        }
      });

      try {
        serviceA.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            try {
              assertFalse(clean);
              barrier.await();
              Thread.sleep(100);
              throw new IllegalStateException();
            } catch (InterruptedException ex) {
              throw new AssertionError(ex);
            } catch (BrokenBarrierException ex) {
              throw new AssertionError(ex);
            }
          }
        });
        fail("Expected ExecutionException");
      } catch (ExecutionException e) {
        assertThat(e.getCause(), instanceOf(IllegalStateException.class));
      }
      
      assertThat(other.get(), is(1));
    } finally {
      serviceA.close();
    }
  }

  @Test
  public void testWinsAfterClosedService() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService serviceA = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
          return 1;
        }
      }), is(1));
    } finally {
      serviceA.close();
    }
      
    CoordinationService serviceB = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceB.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertTrue(clean);
          return 2;
        }
      }), is(2));
    } finally {
      serviceB.close();
    }
  }
}
