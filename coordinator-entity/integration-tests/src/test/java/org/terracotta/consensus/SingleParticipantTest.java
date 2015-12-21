/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.terracotta.consensus;

import java.util.concurrent.Callable;
import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.passthrough.PassthroughServer;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.terracotta.consensus.TestUtils.createServer;

/**
 *
 * @author cdennis
 */
public class SingleParticipantTest {
 
  @Test
  public void testSingleParticipantWins() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService service = new CoordinationService(server.connectNewClient());
    try {
      assertThat(service.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 42;
        }
      }), is(42));
    } finally {
      service.close();
    }
  }
  
  @Test
  public void testSingleParticipantAborts() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService service = new CoordinationService(server.connectNewClient());
    try {
      try {
        service.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
            throw new UnsupportedOperationException();
          }
        });
        fail("Expected UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
        //expected
      }
    } finally {
      service.close();
    }
  }

  @Test
  public void testSingleParticipantAbortsThenWins() throws Throwable {
    PassthroughServer server = createServer();

    CoordinationService service = new CoordinationService(server.connectNewClient());
    try {
      try {
        service.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
          public Integer call() throws Exception {
            throw new UnsupportedOperationException();
          }
        });
        fail("Expected UnsupportedOperationException");
      } catch (UnsupportedOperationException e) {
        //expected
      }

      assertThat(service.executeIfLeader(Entity.class, "foo", new Callable<Integer>() {
        public Integer call() throws Exception {
          return 42;
        }
      }), is(42));
    } finally {
      service.close();
    }
  }
}
