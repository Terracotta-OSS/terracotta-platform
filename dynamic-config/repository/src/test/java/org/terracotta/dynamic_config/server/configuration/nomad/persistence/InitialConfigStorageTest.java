/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.terracotta.dynamic_config.api.model.NodeContext;
import org.terracotta.dynamic_config.api.model.Testing;
import org.terracotta.dynamic_config.api.model.Version;

import java.util.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.terracotta.dynamic_config.api.model.Testing.newTestStripe;
import static org.terracotta.testing.ExceptionMatcher.throwing;

@RunWith(MockitoJUnitRunner.class)
public class InitialConfigStorageTest {

  NodeContext topology = new NodeContext(Testing.newTestCluster("bar", newTestStripe("stripe1").addNodes(Testing.newTestNode("node-1", "localhost"))), Testing.N_UIDS[1]);

  @Mock
  private ConfigStorage underlying;

  @Test
  public void getInitialVersion() throws Exception {
    InitialConfigStorage storage = new InitialConfigStorage(underlying);
    assertThat(() -> storage.getConfig(0L), is(throwing(instanceOf(NoSuchElementException.class))));
  }

  @Test(expected = AssertionError.class)
  public void attemptToSaveInitialVersion() throws Exception {
    InitialConfigStorage storage = new InitialConfigStorage(underlying);
    storage.saveConfig(0L, topology);
  }

  @Test
  public void getOtherVersion() throws Exception {
    when(underlying.getConfig(1L)).thenReturn(new Config(topology, Version.CURRENT));

    InitialConfigStorage storage = new InitialConfigStorage(underlying);
    assertEquals(new Config(topology, Version.CURRENT), storage.getConfig(1L));
  }

  @Test
  public void saveOtherVersion() throws Exception {
    InitialConfigStorage storage = new InitialConfigStorage(underlying);
    storage.saveConfig(1L, topology);

    verify(underlying).saveConfig(1L, topology);
  }
}
