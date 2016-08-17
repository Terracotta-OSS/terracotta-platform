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
package org.terracotta.management.tms.entity.server;

import org.terracotta.management.service.monitoring.Mutation;

import static org.terracotta.management.service.monitoring.Mutation.Type.ADDITION;
import static org.terracotta.management.service.monitoring.Mutation.Type.CHANGE;
import static org.terracotta.management.service.monitoring.Mutation.Type.REMOVAL;
import static org.terracotta.monitoring.PlatformMonitoringConstants.CLIENTS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.ENTITIES_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.FETCHED_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.PLATFORM_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.SERVERS_ROOT_NAME;
import static org.terracotta.monitoring.PlatformMonitoringConstants.STATE_NODE_NAME;

/**
 * @author Mathieu Carbou
 */
enum PlatformNotificationType {

  SERVER_ENTITY_CREATED,
  SERVER_ENTITY_DESTROYED,

  SERVER_ENTITY_FETCHED,
  SERVER_ENTITY_UNFETCHED,

  CONNECTION_OPENED,
  CONNECTION_CLOSED,

  SERVER_STATE_CHANGED,

  SERVER_JOINED,
  SERVER_LEFT,

  CLIENT_TAGS_UPDATED,
  CLIENT_CAPABILITIES_UPDATED,
  CLIENT_CONTEXT_CONTAINER_UPDATED,

  OTHER;

  static String getType(Mutation mutation) {
    if (mutation.pathMatches(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, "*") && mutation.getType() == ADDITION) {
      return CONNECTION_OPENED.name();
    }
    if (mutation.pathMatches(PLATFORM_ROOT_NAME, CLIENTS_ROOT_NAME, "*") && mutation.getType() == REMOVAL) {
      return CONNECTION_CLOSED.name();
    }

    if (mutation.pathMatches("management", "clients", "*", "tags") && mutation.isAnyType(ADDITION, CHANGE)) {
      return CLIENT_TAGS_UPDATED.name();
    }
    if (mutation.pathMatches("management", "clients", "*", "registry", "contextContainer") && mutation.isAnyType(ADDITION, CHANGE)) {
      return CLIENT_CONTEXT_CONTAINER_UPDATED.name();
    }
    if (mutation.pathMatches("management", "clients", "*", "registry", "capabilities") && mutation.isAnyType(ADDITION, CHANGE)) {
      return CLIENT_CAPABILITIES_UPDATED.name();
    }

    if (mutation.pathMatches(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, "*") && mutation.getType() == ADDITION) {
      return SERVER_ENTITY_FETCHED.name();
    }
    if (mutation.pathMatches(PLATFORM_ROOT_NAME, FETCHED_ROOT_NAME, "*") && mutation.getType() == REMOVAL) {
      return SERVER_ENTITY_UNFETCHED.name();
    }

    if (mutation.pathMatches(PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, "*") && mutation.getType() == ADDITION) {
      return SERVER_JOINED.name();
    }
    if (mutation.pathMatches(PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, "*") && mutation.getType() == REMOVAL) {
      return SERVER_LEFT.name();
    }

    if (mutation.pathMatches(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, "*") && mutation.getType() == ADDITION) {
      return SERVER_ENTITY_CREATED.name();
    }
    if (mutation.pathMatches(PLATFORM_ROOT_NAME, ENTITIES_ROOT_NAME, "*") && mutation.getType() == REMOVAL) {
      return SERVER_ENTITY_DESTROYED.name();
    }

    if (mutation.pathMatches(PLATFORM_ROOT_NAME, SERVERS_ROOT_NAME, "*", STATE_NODE_NAME) && mutation.isAnyType(ADDITION, CHANGE)) {
      return SERVER_STATE_CHANGED.name();
    }

    return OTHER.name();
  }

}
