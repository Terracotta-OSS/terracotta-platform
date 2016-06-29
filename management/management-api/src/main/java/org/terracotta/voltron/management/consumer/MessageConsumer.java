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
package org.terracotta.voltron.management.consumer;

/**
 * Interface provided for a management message consumer.
 *
 * @author RKAV
 */
public interface MessageConsumer<M> {
  /**
   * Setup a callback interface, so that the consumer can get stats and other pushed events
   * whenever it arrives at the management service from the managed objects of different entities
   * running on the stripes and clients.
   *
   * @param callback The callback interface that will be issued when stats/events arrives
   */
  void setupPeriodicManagementMessageCollector(MessageListener<M> callback);
}
