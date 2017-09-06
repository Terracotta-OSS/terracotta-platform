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

  private ClientTrackerImpl<Long, Object> clientTracker = new ClientTrackerImpl<>(null);

  @Test
  public void getMessageTracker() throws Exception {
    Tracker<Object> messageTracker = this.clientTracker.getTracker(1L);
    assertThat(messageTracker, notNullValue());
    assertThat(clientTracker.getTracker(1L), sameInstance(messageTracker));

    assertThat(clientTracker.getTracker(2L), not(sameInstance(messageTracker)));
  }

  @Test
  public void untrackClient() throws Exception {
    Tracker<Object> messageTracker = this.clientTracker.getTracker(1L);

    clientTracker.untrackClient(1L);
    assertThat(clientTracker.getTracker(1L), not(sameInstance(messageTracker)));
  }

}
