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
package org.terracotta.dynamic_config.cli.config_tool;

import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Instant;
import java.util.UUID;

import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;

public class NomadTestHelper {

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState) {
    return discovery(changeState, 1L);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount) {
    return discovery(changeState, mutativeMessageCount, UUID.randomUUID());
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, UUID lastChangeUUID) {
    return discovery(changeState, 1L, lastChangeUUID);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount, UUID uuid) {
    return new DiscoverResponse<>(
        changeState == PREPARED ? NomadServerMode.PREPARED : NomadServerMode.ACCEPTING,
        mutativeMessageCount,
        "testMutationHost",
        "testMutationUser",
        Instant.now(),
        1,
        1,
        new ChangeDetails<>(
            uuid,
            changeState,
            1,
            new SimpleNomadChange("testChange", "testSummary"),
            "testChangeResult",
            "testCreationHost",
            "testCreationUser",
            Instant.now()
        )
    );
  }
}
