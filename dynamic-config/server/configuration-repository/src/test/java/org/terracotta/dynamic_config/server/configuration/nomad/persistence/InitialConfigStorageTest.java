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
package org.terracotta.dynamic_config.server.configuration.nomad.persistence;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class InitialConfigStorageTest {
  @Mock
  private ConfigStorage<String> underlying;

  @Test
  public void getInitialVersion() throws Exception {
    InitialConfigStorage<String> storage = new InitialConfigStorage<>(underlying);
    assertNull(storage.getConfig(0L));
  }

  @Test(expected = AssertionError.class)
  public void attemptToSaveInitialVersion() throws Exception {
    InitialConfigStorage<String> storage = new InitialConfigStorage<>(underlying);
    storage.saveConfig(0L, "config");
  }

  @Test
  public void getOtherVersion() throws Exception {
    when(underlying.getConfig(1L)).thenReturn("config");

    InitialConfigStorage<String> storage = new InitialConfigStorage<>(underlying);
    assertEquals("config", storage.getConfig(1L));
  }

  @Test
  public void saveOtherVersion() throws Exception {
    InitialConfigStorage<String> storage = new InitialConfigStorage<>(underlying);
    storage.saveConfig(1L, "config");

    verify(underlying).saveConfig(1L, "config");
  }
}
