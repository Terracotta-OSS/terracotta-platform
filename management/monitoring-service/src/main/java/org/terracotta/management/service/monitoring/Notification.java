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
package org.terracotta.management.service.monitoring;

/**
 * @author Mathieu Carbou
 */
enum Notification {
  SERVER_ENTITY_CREATED,
  SERVER_ENTITY_RECONFIGURED,
  SERVER_ENTITY_DESTROYED,

  SERVER_ENTITY_FETCHED,
  SERVER_ENTITY_UNFETCHED,

  CLIENT_CONNECTED,
  CLIENT_DISCONNECTED,

  SERVER_JOINED,
  SERVER_LEFT,
  SERVER_STATE_CHANGED,

  LOST_MESSAGES,

  CLIENT_REGISTRY_AVAILABLE,
  CLIENT_TAGS_UPDATED,

  ENTITY_REGISTRY_AVAILABLE
}
