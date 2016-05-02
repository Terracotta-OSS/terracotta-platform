/*
 * Copyright Terracotta, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.terracotta.management.model.cluster;

/**
 * @author Mathieu Carbou
 */
public enum ServerState {

//  Currently the possible transitions for a server are:
//  UNINITIALIZED->SYNCHRONIZING->PASSIVE->ACTIVE
//  UNINITIALIZED->ACTIVE

  UNINITIALIZED,
  SYNCHRONIZING,
  PASSIVE,
  ACTIVE,

  UNKNOWN,;

  public static ServerState parse(String state) {
    if (state == null) {
      return UNKNOWN;
    }
    for (ServerState serverState : values()) {
      if (serverState.name().equalsIgnoreCase(state)) {
        return serverState;
      }
    }
    return UNKNOWN;
  }

}
