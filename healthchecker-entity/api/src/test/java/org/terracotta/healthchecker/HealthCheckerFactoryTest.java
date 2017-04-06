/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.healthchecker;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.terracotta.connection.Connection;
import org.terracotta.connection.entity.EntityRef;

/**
 *
 */
public class HealthCheckerFactoryTest {
  
  public HealthCheckerFactoryTest() {
  }
  
  @BeforeClass
  public static void setUpClass() {
  }
  
  @AfterClass
  public static void tearDownClass() {
  }
  
  @Before
  public void setUp() {
  }
  
  @After
  public void tearDown() {
  }

  /**
   * Test of startHealthChecker method, of class HealthCheckerFactory.
   */
  @Test
  public void testCloseOnException() throws Throwable {
    System.out.println("startHealthChecker");
    Connection connection = mock(Connection.class);

    EntityRef ref = mock(EntityRef.class);
    HealthCheck hc = mock(HealthCheck.class);
    when(ref.fetchEntity(null)).thenReturn(hc);
    final Timer timer = new Timer();
    when(hc.getTimer()).thenReturn(timer);
    when(hc.ping(anyString())).thenThrow(new IllegalStateException("not connected"));
    when(connection.getEntityRef(any(Class.class), anyLong(), anyString())).thenReturn(ref);
    TimeoutManager result = HealthCheckerFactory.startHealthChecker(connection, 60, 60000);
    final Semaphore latch = new Semaphore(0);
    TimerTask tt = new TimerTask() {
      @Override
      public void run() {
        timer.cancel();
        latch.release();
      }
    };
    timer.schedule(tt, 0);
    latch.acquire();
    verify(connection).close();
    try {
      result.addTimeoutListener(new TimeoutListener() {
        @Override
        public void connectionClosed(Connection target) {
          throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void probeFailed(Connection target) {
          throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
      });
      Assert.fail("should throw");
    } catch (IllegalStateException closed) {
//  expected;
    }
  }
  
}
