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

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.hamcrest.Matchers.sameInstance;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.ClientSourceId;

public class ClientTrackerImplTest {

  private ClientTrackerImpl<Object, Object> clientTracker = new ClientTrackerImpl<>();

  @Test
  public void getMessageTracker() throws Exception {
    Tracker<Object, Object> messageTracker = this.clientTracker.getTracker(mockClientId(1L));
    assertThat(messageTracker, notNullValue());
    assertThat(clientTracker.getTracker(mockClientId(1L)), sameInstance(messageTracker));

    assertThat(clientTracker.getTracker(mockClientId(2L)), not(sameInstance(messageTracker)));
  }

  @Test
  public void untrackClient() throws Exception {
    Tracker<Object, Object> messageTracker = this.clientTracker.getTracker(mockClientId(1L));

    clientTracker.untrackClient(mockClientId(1L));
    assertThat(clientTracker.getTracker(mockClientId(1L)), not(sameInstance(messageTracker)));
  }

  private ClientSourceId mockClientId(long id) {
    return new ClientSourceId() {
      @Override
      public long toLong() {
        return id;
      }

      @Override
      public boolean isValidClient() {
        return id >= 0;
      }

      @Override
      public boolean matches(ClientDescriptor cd) {
        return cd.getSourceId().toLong() == id;
      }
      
      @Override
      public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (int)((id >> 32) & 0xffff) + (int)(id & 0xffff);
        return hash;
      }

      @Override
      public boolean equals(Object obj) {
        if (this == obj) {
          return true;
        }
        if (obj == null) {
          return false;
        }
        if (getClass() != obj.getClass()) {
          return false;
        }
        return ((ClientSourceId)obj).toLong() == id;
      }
    };
  }
}
