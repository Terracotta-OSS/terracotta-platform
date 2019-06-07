/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */
package com.terracottatech.nomad.client.change;

import com.terracottatech.nomad.client.results.CommitRollbackResultsReceiver;
import com.terracottatech.nomad.client.results.DiscoverResultsReceiver;
import com.terracottatech.nomad.client.results.PrepareResultsReceiver;
import com.terracottatech.nomad.client.results.ServerPreparedResultsReceiver;

public interface ChangeResultReceiver extends DiscoverResultsReceiver, ServerPreparedResultsReceiver, PrepareResultsReceiver, CommitRollbackResultsReceiver {
}
