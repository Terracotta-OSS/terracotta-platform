/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.port_locking;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.Channel;
import java.nio.channels.FileLock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class GlobalFilePortLockTest {
  @Mock
  private RandomAccessFile file;

  @Mock
  private Channel channel;

  @Mock
  private FileLock lock;

  @Test
  public void happyPath() throws Exception {
    PortLock portLock = new GlobalFilePortLock(1, file, channel, lock);
    assertEquals(1, portLock.getPort());
    portLock.close();
    verify(file).close();
    verify(channel).close();
    verify(lock).close();
  }

  @Test
  public void closesThrow() throws Exception {
    doThrow(IOException.class).when(file).close();
    doThrow(IOException.class).when(channel).close();
    doThrow(IOException.class).when(lock).close();
    PortLock portLock = new GlobalFilePortLock(1, file, channel, lock);
    assertEquals(1, portLock.getPort());
    try {
      portLock.close();
      fail("Expected PortLockingException");
    } catch (PortLockingException e) {
      // Expected
    }
    verify(file).close();
    verify(channel).close();
    verify(lock).close();
  }
}
