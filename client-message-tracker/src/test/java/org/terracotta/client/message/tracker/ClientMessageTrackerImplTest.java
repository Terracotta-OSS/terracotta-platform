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
package org.terracotta.client.message.tracker;

import org.junit.Test;
import org.terracotta.entity.ClientDescriptor;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class ClientMessageTrackerImplTest {

  private ClientMessageTrackerImpl clientMessageTracker = new ClientMessageTrackerImpl(mock(TrackerPolicy.class));

  @Test
  public void getMessageTracker() throws Exception {
    ClientDescriptor descriptor1 = new DummyClientDescriptor(1);
    MessageTracker messageTracker = this.clientMessageTracker.getMessageTracker(descriptor1);
    assertThat(messageTracker, notNullValue());
    assertThat(clientMessageTracker.getMessageTracker(descriptor1), sameInstance(messageTracker));

    ClientDescriptor descriptor2 = new DummyClientDescriptor(2);
    assertThat(clientMessageTracker.getMessageTracker(descriptor2), not(sameInstance(messageTracker)));
  }

  @Test
  public void untrackClient() throws Exception {
    ClientDescriptor descriptor = new DummyClientDescriptor(1);
    MessageTracker messageTracker = this.clientMessageTracker.getMessageTracker(descriptor);

    clientMessageTracker.untrackClient(descriptor);
    assertThat(clientMessageTracker.getMessageTracker(descriptor), not(sameInstance(messageTracker)));
  }

}