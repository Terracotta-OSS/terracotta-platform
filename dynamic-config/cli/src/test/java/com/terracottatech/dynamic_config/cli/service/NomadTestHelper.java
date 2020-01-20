/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.dynamic_config.cli.service;

import com.terracottatech.nomad.client.change.SimpleNomadChange;
import com.terracottatech.nomad.messages.ChangeDetails;
import com.terracottatech.nomad.messages.DiscoverResponse;
import com.terracottatech.nomad.server.ChangeRequestState;
import com.terracottatech.nomad.server.NomadServerMode;

import java.time.Instant;
import java.util.Collections;
import java.util.UUID;

import static com.terracottatech.nomad.server.ChangeRequestState.PREPARED;

public class NomadTestHelper {

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState) {
    return discovery(changeState, 1L);
  }

  public static DiscoverResponse<String> discovery(ChangeRequestState changeState, long mutativeMessageCount) {
    return discovery(changeState, mutativeMessageCount, UUID.randomUUID());
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
        ),
        Collections.emptyList()
    );
  }
}
