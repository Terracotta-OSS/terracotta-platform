/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.results;

import java.net.InetSocketAddress;
import java.util.UUID;

public interface PrepareResultsReceiver {
  default void startPrepare(UUID newChangeUuid) {}

  default void prepared(InetSocketAddress server) {}

  default void prepareFail(InetSocketAddress server, String reason) {}

  default void prepareOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {}

  default void prepareChangeUnacceptable(InetSocketAddress server, String rejectionReason) {}

  default void endPrepare() {}
}
