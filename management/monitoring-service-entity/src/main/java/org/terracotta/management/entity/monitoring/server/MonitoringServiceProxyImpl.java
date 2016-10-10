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
package org.terracotta.management.entity.monitoring.server;

import org.terracotta.management.entity.monitoring.MonitoringServiceProxy;
import org.terracotta.management.model.cluster.Cluster;
import org.terracotta.management.model.message.Message;
import org.terracotta.management.service.monitoring.MonitoringService;
import org.terracotta.management.service.monitoring.buffer.ReadOnlyBuffer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Mathieu Carbou
 */
class MonitoringServiceProxyImpl implements MonitoringServiceProxy {

  private final MonitoringService monitoringService;
  private ReadOnlyBuffer<Message> messageBuffer;

  MonitoringServiceProxyImpl(MonitoringService monitoringService) {
    this.monitoringService = monitoringService;
  }

  @Override
  public Cluster readTopology() {
    return monitoringService.readTopology();
  }

  @Override
  public void createMessageBuffer(int size) {
    messageBuffer = monitoringService.createMessageBuffer(size);
  }

  @Override
  public Message readMessageBuffer() {
    return messageBuffer.read();
  }

  @Override
  public List<Message> drainMessageBuffer() {
    List<Message> messages = new ArrayList<>(messageBuffer.size());
    messageBuffer.drainTo(messages);
    return messages;
  }

  @Override
  public void clearMessageBuffer() {
    messageBuffer.clear();
  }

}
