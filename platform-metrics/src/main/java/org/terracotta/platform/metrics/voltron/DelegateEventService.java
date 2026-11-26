/*
 * Copyright Terracotta, Inc.
 * Copyright IBM Corp. 2026
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
package org.terracotta.platform.metrics.voltron;

import org.terracotta.event.EventService;

import java.util.Map;

/**
 * A delegating wrapper around an {@link EventService}.
 *
 * <p>Subclasses can override individual methods to intercept or augment specific events
 * while forwarding everything else to the wrapped service.
 */
public class DelegateEventService implements EventService {
  /** The underlying {@link EventService} that all calls are forwarded to. */
  protected final EventService eventService;

  /**
   * Creates a delegate that forwards all calls to {@code eventService}.
   *
   * @param eventService the service to delegate to (must not be {@code null})
   */
  public DelegateEventService(EventService eventService) {
    this.eventService = eventService;
  }

  @Override
  public void fireEvent(Event event, Map<String, String> map) {
    eventService.fireEvent(event, map);
  }

  @Override
  public void fireEvent(Event event) {
    eventService.fireEvent(event);
  }

  @Override
  public String toString() {
    return "Delegate of: " + eventService;
  }
}
