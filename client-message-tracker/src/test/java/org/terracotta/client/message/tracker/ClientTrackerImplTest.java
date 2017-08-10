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
import org.terracotta.entity.ClientSourceId;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class ClientTrackerImplTest {

  private ClientTrackerImpl clientTracker = new ClientTrackerImpl(mock(TrackerPolicy.class));

  @Test
  public void getMessageTracker() throws Exception {
    ClientSourceId descriptor1 = new DummyClientSourceId(1);
    Tracker messageTracker = this.clientTracker.getTracker(descriptor1);
    assertThat(messageTracker, notNullValue());
    assertThat(clientTracker.getTracker(descriptor1), sameInstance(messageTracker));

    ClientSourceId descriptor2 = new DummyClientSourceId(2);
    assertThat(clientTracker.getTracker(descriptor2), not(sameInstance(messageTracker)));
  }

  @Test
  public void untrackClient() throws Exception {
    ClientSourceId descriptor = new DummyClientSourceId(1);
    Tracker messageTracker = this.clientTracker.getTracker(descriptor);

    clientTracker.untrackClient(descriptor);
    assertThat(clientTracker.getTracker(descriptor), not(sameInstance(messageTracker)));
  }

}
