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


package org.terracotta.consensus.entity.integration;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;
import org.terracotta.connection.Connection;
import org.terracotta.consensus.CoordinationService;
import org.terracotta.consensus.entity.CoordinationServerEntityService;
import org.terracotta.consensus.entity.client.ClientCoordinationEntityService;
import org.terracotta.consensus.nomination.NominationConsumer;
import org.terracotta.consensus.testentity.common.TestCoordination;
import org.terracotta.passthrough.PassthroughServer;

import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.is;

public class SingleEntityBasicTest {
  
  private static final String ENTITY_NAME = "entity1";
 
  @Test
  public void testSingleClientConnection() throws Throwable {

    PassthroughServer activeServer = new PassthroughServer(true);
    
    CoordinationServerEntityService coordinationServerEntityService = new CoordinationServerEntityService();
    ClientCoordinationEntityService clientCoordinationEntityService = new ClientCoordinationEntityService();
    
    activeServer.registerClientEntityService(clientCoordinationEntityService);
    activeServer.registerServerEntityService(coordinationServerEntityService);
    
    activeServer.start();
   
    Connection connection = activeServer.connectNewClient();
    
    CoordinationService coordinationService = new CoordinationService(connection);
    
    String result = coordinationService.executeIfLeader(TestCoordination.class, ENTITY_NAME, new Callable<String>() {

      public String call() throws Exception {
        return "I am the Leader !";
      }
    }, new NominationConsumer() {
      
      public void onNominationByServer(Object result) {
        
      }
      
      public Callable consumeNomination() {
        return null;
      }
    });
    
    assertThat(result, is("I am the Leader !"));
    
  }
  
  @Test
  public void testContinuingClientCoonectionAfterLeaderDelists() throws Throwable {
    PassthroughServer activeServer = new PassthroughServer(true);
    
    CoordinationServerEntityService coordinationServerEntityService = new CoordinationServerEntityService();
    ClientCoordinationEntityService clientCoordinationEntityService = new ClientCoordinationEntityService();
    
    activeServer.registerClientEntityService(clientCoordinationEntityService);
    activeServer.registerServerEntityService(coordinationServerEntityService);
    
    activeServer.start();
   
    Connection connection = activeServer.connectNewClient();
    
    CoordinationService coordinationService = new CoordinationService(connection);
    
    String result = coordinationService.executeIfLeader(TestCoordination.class, ENTITY_NAME, new Callable<String>() {

      public String call() throws Exception {
        return "I am the Leader !";
      }
    }, new NominationConsumer() {
      
      public void onNominationByServer(Object result) {
        
      }
      
      public Callable consumeNomination() {
        return null;
      }
    });
    
    assertThat(result, is("I am the Leader !"));
    
    coordinationService.delist(TestCoordination.class, ENTITY_NAME);
    
    Connection newClient = activeServer.connectNewClient();
    
    CoordinationService coordinationService2 = new CoordinationService(newClient);

    String shouldBeLeader = coordinationService2.executeIfLeader(TestCoordination.class, ENTITY_NAME, new Callable<String>() {

      public String call() throws Exception {
        return "I am the Leader !";
      }
    }, new NominationConsumer() {
      
      public void onNominationByServer(Object result) {
        
      }
      
      public Callable consumeNomination() {
        return null;
      }
    });

    assertThat(shouldBeLeader, is("I am the Leader !"));
  }
  
  @Test
  public void testMultipleClientConnections() throws Throwable {
    
    PassthroughServer activeServer = new PassthroughServer(true);
    
    CoordinationServerEntityService coordinationServerEntityService = new CoordinationServerEntityService();
    ClientCoordinationEntityService clientCoordinationEntityService = new ClientCoordinationEntityService();
    
    activeServer.registerClientEntityService(clientCoordinationEntityService);
    activeServer.registerServerEntityService(coordinationServerEntityService);
    
    activeServer.start();
    
    Map<String, CoordinationService> map = new HashMap<String, CoordinationService>();
    Map<String, String> resultMap = new HashMap<String, String>();
    Map<String, NominationConsumer> consumers = new HashMap<String, NominationConsumer>();
    
    for (int i = 0; i < 3; i++) {
      map.put("c" + i, new CoordinationService(activeServer.connectNewClient()));
    }
    
    final AtomicInteger consumerCounter = new AtomicInteger(0);
    final CountDownLatch latch = new CountDownLatch(1);
    
    for (String cs : map.keySet()) {

      NominationConsumer consumer = new NominationConsumer() {

        public void onNominationByServer(Object result) {
          consumerCounter.incrementAndGet();
          assertThat(result.toString(), is("I am nominated !. Do different."));
          latch.countDown();
        }

        public Callable consumeNomination() {
          return new Callable() {

            public Object call() throws Exception {
              return "I am nominated !. Do different.";
            }
          };
        }
      };

      consumers.put(cs, consumer);

      resultMap.put(cs, map.get(cs).executeIfLeader(TestCoordination.class, ENTITY_NAME, new Callable<String>() {

                public String call() throws Exception {
                  return "I am the leader !";
                }
              }, consumer));
    }
    
    assertThat(resultMap.containsValue("I am the leader !"), is(true));
    
    Iterable<String> iterable = resultMap.values();
    
    int onlyOnceCount = 0;
    
    for (String string : iterable) {
      if(string != null && string.equals("I am the leader !")) {
        onlyOnceCount++;
      }
    }
    assertThat(onlyOnceCount, is(1));
    
    for (Entry<String, String> cs : resultMap.entrySet()) {
      if(cs.getValue() != null && cs.getValue().equals("I am the leader !")) {
        map.get(cs.getKey()).delist(TestCoordination.class, ENTITY_NAME);
      }
    }
    
    latch.await();
    
    assertThat(consumerCounter.get(), is(1));
    
  }

}
