/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.recovery;

import org.terracotta.nomad.client.results.CommitRollbackResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.TakeoverResultsReceiver;

public interface RecoveryResultReceiver<T> extends DiscoverResultsReceiver<T>, TakeoverResultsReceiver, CommitRollbackResultsReceiver {
}
