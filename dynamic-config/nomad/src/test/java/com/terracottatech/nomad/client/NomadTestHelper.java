/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client;

import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

public class NomadTestHelper {
  @SuppressWarnings("unchecked")
  public static <T> Set<T> setOf(T... values) {
    return new HashSet<>(Arrays.asList(values));
  }

  @SuppressWarnings("unchecked")
  public static <T> Set<T> matchSetOf(T... values) {
    return (Set<T>) argThat(containsInAnyOrder(values));
  }

  public static DiscoverResponse discovery(ChangeRequestState changeState) {
    return discovery(changeState, 1L);
  }

  public static DiscoverResponse discovery(ChangeRequestState changeState, long mutativeMessageCount) {
    return discovery(changeState, mutativeMessageCount, UUID.randomUUID());
  }

  public static DiscoverResponse discovery(ChangeRequestState changeState, UUID uuid) {
    return discovery(changeState, 1L, uuid);
  }

  public static DiscoverResponse discovery(ChangeRequestState changeState, long mutativeMessageCount, UUID uuid) {
    return new DiscoverResponse(
        changeState == PREPARED ? NomadServerMode.PREPARED : NomadServerMode.ACCEPTING,
        mutativeMessageCount,
        "testMutationHost",
        "testMutationUser",
        1,
        1,
        new ChangeDetails(
            uuid,
            changeState,
            1,
            new SimpleNomadChange("testChange", "testSummary"),
            "testChangeResult",
            "testCreationHost",
            "testCreationUser"
        )
    );
  }
}
