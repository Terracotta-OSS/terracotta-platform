/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terracotta.management.service.impl;

import org.terracotta.management.service.buffer.PartitionedRingBuffer;
import org.terracotta.voltron.management.producer.MessageProducer;

/**
 * The management service producer.
 * <p>
 * TODO: as of now only the management message producer is implemented mainly for the PoC.
 * Other interfaces will be implemented later.
 *
 *
 * @author RKAV
 */
public class DefaultMessageProducer<M> implements MessageProducer<M> {
  private final PartitionedRingBuffer<M> messageCache;
  private final int myPartitionNumber;

  public DefaultMessageProducer(PartitionedRingBuffer<M> messageCache, int partitionNo) {
    this.messageCache = messageCache;
    this.myPartitionNumber = partitionNo;
  }

  @Override
  public void pushManagementMessage(M message) {
    messageCache.insert(myPartitionNumber, message);
  }
}
