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
package org.terracotta.voltron.management.producer;

/**
 * Interface for managed entities to push management messages for interested consumers.
 *
 * @author RKAV
 */
public interface MessageProducer<M> {
  /**
   * Push messages (could be periodic statistics or irregular operator events or
   * even simple serialized byte arrays)
   * <p>
   * It is assumed that the message object has sufficient context for the management
   * system to identify the entity instance and object instance for which this statistic
   * or operator event is pushed.
   *
   * @param message A management message that is periodically pushed.
   */
  void pushManagementMessage(M message);
}
