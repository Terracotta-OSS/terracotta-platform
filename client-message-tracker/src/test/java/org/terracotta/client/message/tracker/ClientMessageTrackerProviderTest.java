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
import org.terracotta.entity.ServiceConfiguration;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class ClientMessageTrackerProviderTest {

  @Test
  public void getService() throws Exception {
    ClientMessageTrackerProvider provider =  new ClientMessageTrackerProvider();
    ClientMessageTrackerConfiguration trackerConfiguration = new ClientMessageTrackerConfiguration("foo", mock(TrackerPolicy.class));
    ClientMessageTracker tracker = provider.getService(1L, trackerConfiguration);
    assertThat(provider.getService(2L, trackerConfiguration), sameInstance(tracker));

    trackerConfiguration = new ClientMessageTrackerConfiguration("bar", mock(TrackerPolicy.class));
    assertThat(provider.getService(2L, trackerConfiguration), not(sameInstance(tracker)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getServiceInvalidConfig() throws Exception {
    ClientMessageTrackerProvider provider =  new ClientMessageTrackerProvider();
    provider.getService(1L, mock(ServiceConfiguration.class));
  }

}