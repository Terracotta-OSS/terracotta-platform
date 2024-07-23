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
package org.terracotta.client.message.tracker;

import org.junit.Test;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

public class TrackerImplTest {

  @Test
  public void trackTrackableMessage() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    TrackerImpl<EntityMessage, EntityResponse> tracker = new TrackerImpl<>();
    tracker.track(1L, 1L, message, response);

    assertThat(tracker.getTrackedValue(1L), sameInstance(response));
  }

  @Test
  public void trackInvalidMessage() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityMessage, EntityResponse> tracker = new TrackerImpl<>();
    tracker.track(1L, -1L, message, response);  // a message with non-positive message id

    assertThat(tracker.getTrackedValue(-1L), nullValue());
  }

  @Test
  public void reconcile() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    TrackerImpl<EntityMessage, EntityResponse> tracker = new TrackerImpl<>();
    tracker.track(1L, 1L, message, response);
    tracker.track(2L, 2L, message, response);
    tracker.track(3L, 3L, message, response);

    assertThat(tracker.getTrackedValue(1L), notNullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());

    tracker.reconcile(1L);
    assertThat(tracker.getTrackedValue(1L), notNullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());

    tracker.reconcile(3L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), nullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());
  }

  @Test
  public void testDuplicateReconcile() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityMessage, EntityResponse> tracker = new TrackerImpl<>();
    tracker.track(1L, 1L, message, response);
    tracker.track(2L, 2L, message, response);
    tracker.track(3L, 3L, message, response);

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());

  }
}
