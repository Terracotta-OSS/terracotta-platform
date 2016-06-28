/**
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
package org.terracotta.consensus;

import java.util.concurrent.ExecutionException;
import org.junit.Test;
import org.terracotta.connection.entity.Entity;
import org.terracotta.consensus.CoordinationService.ElectionTask;
import org.terracotta.passthrough.PassthroughServer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertFalse;
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
      assertThat(service.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
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
        service.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            assertFalse(clean);
            throw new UnsupportedOperationException();
          }
        });
        fail("Expected ExecutionException");
      } catch (ExecutionException e) {
        assertThat(e.getCause(), instanceOf(UnsupportedOperationException.class));
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
        service.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
          public Integer call(boolean clean) {
            assertFalse(clean);
            throw new UnsupportedOperationException();
          }
        });
        fail("Expected ExecutionException");
      } catch (ExecutionException e) {
        assertThat(e.getCause(), instanceOf(UnsupportedOperationException.class));
      }

      assertThat(service.executeIfLeader(Entity.class, "foo", new ElectionTask<Integer>() {
        public Integer call(boolean clean) {
          assertFalse(clean);
          return 42;
        }
      }), is(42));
    } finally {
      service.close();
    }
  }
}
