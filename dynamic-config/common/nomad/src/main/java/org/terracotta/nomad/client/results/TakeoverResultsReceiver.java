/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.results;

import java.net.InetSocketAddress;

public interface TakeoverResultsReceiver {
  default void startTakeover() {}

  default void takeover(InetSocketAddress server) {}

  default void takeoverOtherClient(InetSocketAddress server, String lastMutationHost, String lastMutationUser) {}

  default void takeoverFail(InetSocketAddress server, String reason) {}

  default void endTakeover() {}
}
