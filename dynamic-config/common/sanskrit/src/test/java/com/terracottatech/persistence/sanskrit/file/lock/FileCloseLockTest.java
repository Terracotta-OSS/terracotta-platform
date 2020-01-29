/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.persistence.sanskrit.file.lock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.nio.channels.Channel;
import java.nio.channels.FileLock;

import static org.mockito.Mockito.inOrder;

@RunWith(MockitoJUnitRunner.class)
public class FileCloseLockTest {
  @Mock
  private Channel channel;

  @Mock
  private FileLock fileLock;

  @Test
  public void closesLockAndChannel() throws Exception {
    FileCloseLock lock = new FileCloseLock(channel, fileLock);
    lock.close();

    InOrder inOrder = inOrder(channel, fileLock);

    inOrder.verify(fileLock).close();
    inOrder.verify(channel).close();
  }
}
