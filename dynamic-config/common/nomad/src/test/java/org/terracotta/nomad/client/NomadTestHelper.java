/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client;

import org.terracotta.nomad.client.change.SimpleNomadChange;
import org.terracotta.nomad.messages.ChangeDetails;
import org.terracotta.nomad.messages.DiscoverResponse;
import org.terracotta.nomad.server.ChangeRequestState;
import org.terracotta.nomad.server.NomadServerMode;

import java.time.Clock;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;
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
    return discovery(changeState, mutativeMessageCount, UUID.randomUUID());
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, UUID uuid) {
    return discovery(changeState, 1L, uuid);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount, UUID uuid) {
    return new DiscoverResponse<>(
        changeState == PREPARED ? NomadServerMode.PREPARED : NomadServerMode.ACCEPTING,
        mutativeMessageCount,
        "testMutationHost",
        "testMutationUser",
        Clock.systemDefaultZone().instant(),
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
            Clock.systemDefaultZone().instant()
        ),
        Collections.emptyList()
    );
  }
}