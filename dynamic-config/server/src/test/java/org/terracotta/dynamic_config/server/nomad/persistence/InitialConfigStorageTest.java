/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.dynamic_config.server.nomad.persistence;

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
