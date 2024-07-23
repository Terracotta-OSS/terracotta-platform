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
package org.terracotta.persistence.sanskrit.file.lock;

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
