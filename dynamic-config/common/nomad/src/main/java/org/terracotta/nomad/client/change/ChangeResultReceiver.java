/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package org.terracotta.nomad.client.change;

import org.terracotta.nomad.client.results.CommitRollbackResultsReceiver;
import org.terracotta.nomad.client.results.DiscoverResultsReceiver;
import org.terracotta.nomad.client.results.PrepareResultsReceiver;
import org.terracotta.nomad.client.results.ServerPreparedResultsReceiver;

public interface ChangeResultReceiver<T> extends DiscoverResultsReceiver<T>, ServerPreparedResultsReceiver, PrepareResultsReceiver, CommitRollbackResultsReceiver {
}
