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
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.EntityResponse;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class TrackerImplTest {

  @Test
  public void trackTrackableMessage() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityResponse> tracker = new TrackerImpl<>(o -> true);
    tracker.track(1L, message, response);

    assertThat(tracker.getTrackedValue(1L), sameInstance(response));
  }

  @Test
  public void trackUnTrackableMessage() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityResponse> tracker = new TrackerImpl<>(o -> false);
    tracker.track(1L, message, response);

    assertThat(tracker.getTrackedValue(1L), nullValue());
  }

  @Test
  public void trackInvalidMessage() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityResponse> tracker = new TrackerImpl<>(o -> true);
    tracker.track(-1L, message, response);  // a message with non-positive message id

    assertThat(tracker.getTrackedValue(-1L), nullValue());
  }

  @Test
  public void reconcile() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    TrackerImpl<EntityResponse> tracker = new TrackerImpl<>(o -> true);
    tracker.track(1L, message, response);
    tracker.track(2L, message, response);
    tracker.track(3L, message, response);

    assertThat(tracker.getTrackedValue(1L), notNullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());

    tracker.reconcile(1L);
    assertThat(tracker.getTrackedValue(1L), notNullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());
    assertThat(tracker.getLastReconciledId(), is(1L));

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());
    assertThat(tracker.getLastReconciledId(), is(2L));

    tracker.reconcile(3L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), nullValue());
    assertThat(tracker.getTrackedValue(3L), notNullValue());
    assertThat(tracker.getLastReconciledId(), is(3L));
  }

  @Test
  public void testDuplicateReconcile() throws Exception {
    EntityMessage message = mock(EntityMessage.class);
    EntityResponse response = mock(EntityResponse.class);

    Tracker<EntityResponse> tracker = new TrackerImpl<>(o -> true);
    tracker.track(1L, message, response);
    tracker.track(2L, message, response);
    tracker.track(3L, message, response);

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());

    tracker.reconcile(2L);
    assertThat(tracker.getTrackedValue(1L), nullValue());
    assertThat(tracker.getTrackedValue(2L), notNullValue());

  }

  @Test
  public void testLoadOnSync() throws Exception {
    TrackerImpl<Object> tracker = new TrackerImpl<>(o -> true);
    Map<Long, Object> responses = Collections.singletonMap(1L, "value");
    tracker.loadOnSync(responses);
    Map<Long, Object> actual = tracker.getTrackedValues();
    assertThat(actual.size(), is(1));
    assertThat(actual.get(1L), is("value"));
  }
}
