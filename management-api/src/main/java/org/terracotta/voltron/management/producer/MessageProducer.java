/*
 *
 *  The contents of this file are subject to the Terracotta Public License Version
 *  2.0 (the "License"); You may not use this file except in compliance with the
 *  License. You may obtain a copy of the License at
 *
 *  http://terracotta.org/legal/terracotta-public-license.
 *
 *  Software distributed under the License is distributed on an "AS IS" basis,
 *  WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 *  the specific language governing rights and limitations under the License.
 *
 *  The Covered Software is Entity Management API.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
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
