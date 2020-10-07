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
package org.terracotta.nomad.client;

import org.terracotta.nomad.SimpleNomadChange;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Clock;
import java.util.Collection;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
import static org.terracotta.nomad.server.ChangeRequestState.COMMITTED;
import static org.terracotta.nomad.server.ChangeRequestState.PREPARED;

public class NomadTestHelper {
  @SuppressWarnings("unchecked")
  public static <T> Collection<T> withItems(T... values) {
    return (Collection<T>) argThat(containsInAnyOrder(values));
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState) {
    return discovery(changeState, 1L);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount) {
    return discovery(changeState, mutativeMessageCount, UUID.randomUUID(), "testChangeResultHash");
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, UUID uuid) {
    return discovery(changeState, 1L, uuid, "testChangeResultHash");
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, UUID uuid, String changeResultHash) {
    return discovery(changeState, 1L, uuid, changeResultHash);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount, UUID uuid, String changeResultHash) {
    return discovery(changeState, mutativeMessageCount, uuid, changeResultHash, changeState == COMMITTED ? uuid : UUID.randomUUID(), "hash");
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, UUID uuid, String changeResultHash, UUID lastCommittedChangeUid, String lastCommittedChangeResultHash) {
    return discovery(changeState, 1L, uuid, changeResultHash, lastCommittedChangeUid, lastCommittedChangeResultHash);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount, UUID uuid, String changeResultHash, UUID lastCommittedChangeUid, String lastCommittedChangeResultHash) {
    final ChangeDetails<String> changeDetails = new ChangeDetails<>(
        uuid,
        changeState,
        2,
        new SimpleNomadChange("testChange", "testSummary"),
        "testChangeResult",
        "testCreationHost",
        "testCreationUser",
        Clock.systemDefaultZone().instant(),
        changeResultHash
    );
    return new DiscoverResponse<>(
        changeState == PREPARED ? NomadServerMode.PREPARED : NomadServerMode.ACCEPTING,
        mutativeMessageCount,
        "testMutationHost",
        "testMutationUser",
        Clock.systemDefaultZone().instant(),
        2,
        changeState == COMMITTED ? 2 : 1,
        changeDetails,
        lastCommittedChangeUid == null ? null : lastCommittedChangeUid.equals(uuid) ? changeDetails : new ChangeDetails<>(
            lastCommittedChangeUid,
            COMMITTED,
            1,
            new SimpleNomadChange("testChange1", "testSummary1"),
            "testChangeResult1",
            "testCreationHost1",
            "testCreationUser1",
            Clock.systemDefaultZone().instant(),
            lastCommittedChangeResultHash
        ));
  }
}
