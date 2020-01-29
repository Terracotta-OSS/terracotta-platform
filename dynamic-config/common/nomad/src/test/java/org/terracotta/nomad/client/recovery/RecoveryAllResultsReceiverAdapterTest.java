/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.recovery;

import org.junit.Test;
import org.terracotta.nomad.client.AllResultsReceiverAdapterTest;

public class RecoveryAllResultsReceiverAdapterTest extends AllResultsReceiverAdapterTest<RecoveryResultReceiver<String>, RecoveryAllResultsReceiverAdapter<String>> {
  @SuppressWarnings("unchecked")
  @Test
  public void checkAllMethodsDelegateCorrectly() throws Exception {
    runTest((Class<RecoveryResultReceiver<String>>) (Class<?>) RecoveryResultReceiver.class, (Class<RecoveryAllResultsReceiverAdapter<String>>) (Class<?>) RecoveryAllResultsReceiverAdapter.class);
  }
}
