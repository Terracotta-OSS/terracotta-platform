/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus;

import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Future;
import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.passthrough.PassthroughServer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
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
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 1;
        }
      }), is(1));
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
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
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 1;
        }
      }), is(1));
      serviceA.delist(Entity.class, "foo");
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
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
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 1;
        }
      }), is(1));
      
      CoordinationService serviceB = new CoordinationService(server.connectNewClient());
      try {
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
            throw new AssertionError();
          }
        }), nullValue());
        
        serviceA.delist(Entity.class, "foo");
        
        assertThat(serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
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
            return serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
              public Integer call() {
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
        serviceA.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
            barrier.await();
            Thread.sleep(100);
            throw new IllegalStateException();
          }
        });
        fail("Expected IllegalStateException");
      } catch (IllegalStateException e) {
        //expected
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
      assertThat(serviceA.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 1;
        }
      }), is(1));
    } finally {
      serviceA.close();
    }
      
    CoordinationService serviceB = new CoordinationService(server.connectNewClient());
    try {
      assertThat(serviceB.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 2;
        }
      }), is(2));
    } finally {
      serviceB.close();
    }
  }
}
