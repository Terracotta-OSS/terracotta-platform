/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.results;

import java.util.UUID;

public interface PrepareResultsReceiver {
  default void startPrepare(UUID newChangeUuid) {}

  default void prepared(String server) {}

  default void prepareFail(String server, String reason) {}

  default void prepareOtherClient(String server, String lastMutationHost, String lastMutationUser) {}

  default void prepareChangeUnacceptable(String server, String rejectionReason) {}

  default void endPrepare() {}
}
