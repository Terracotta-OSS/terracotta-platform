/**
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
package org.terracotta.management.entity.monitoringconsumer.server;

import org.terracotta.management.entity.monitoringconsumer.MonitoringConsumer;
import org.terracotta.management.service.monitoring.IMonitoringConsumer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Mathieu Carbou
 */
class MonitoringConsumerImpl implements MonitoringConsumer {

  private final IMonitoringConsumer monitoringConsumer;

  MonitoringConsumerImpl(IMonitoringConsumer monitoringConsumer) {
    this.monitoringConsumer = monitoringConsumer;
  }

  @Override
  public Collection<String> getChildNamesForNode(String[] parent, String nodeName) {
    return monitoringConsumer.getChildNamesForNode(parent, nodeName).map(ArrayList::new).orElse(null);
  }

  @Override
  public Object getValueForNode(String[] path) {
    return monitoringConsumer.getValueForNode(path, Object.class).orElse(null);
  }

  @Override
  public Object getValueForNode(String[] parents, String nodeName) {
    return monitoringConsumer.getValueForNode(parents, nodeName, Object.class).orElse(null);
  }

  @Override
  public Collection<String> getChildNamesForNode(String... path) {
    return monitoringConsumer.getChildNamesForNode(path).map(ArrayList::new).orElse(null);
  }

  @Override
  public Map<String, Object> getChildValuesForNode(String... path) {
    return monitoringConsumer.getChildValuesForNode(path).map(HashMap::new).orElse(null);
  }

  @Override
  public Map<String, Object> getChildValuesForNode(String[] parent, String nodeName) {
    return monitoringConsumer.getChildValuesForNode(parent, nodeName).map(HashMap::new).orElse(null);
  }

}
