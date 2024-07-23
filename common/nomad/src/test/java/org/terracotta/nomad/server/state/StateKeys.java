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
package org.terracotta.nomad.server.state;

public interface StateKeys {
  String INITIALIZED = "initialized";
  String REQUEST = "request";
  String MODE = "mode";
  String MUTATIVE_MESSAGE_COUNT = "mutativeMessageCount";
  String LAST_MUTATION_HOST = "lastMutationHost";
  String LAST_MUTATION_USER = "lastMutationUser";
  String LAST_MUTATION_TIMESTAMP = "lastMutationTimestamp";
  String LATEST_CHANGE_UUID = "latestChangeUuid";
  String CURRENT_VERSION = "currentVersion";
  String HIGHEST_VERSION = "highestVersion";

  String STATE = "state";
  String VERSION = "version";
  String CHANGE = "change";
  String CREATION_HOST = "creationHost";
  String CREATION_USER = "creationUser";
  String CREATION_TIMESTAMP = "creationTimestamp";
  String PREV_CHANGE_UUID = "prevChangeUuid";
}
