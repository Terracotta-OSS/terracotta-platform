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
package org.terracotta.voltron.management;

/**
 * Management Service Interface.
 * <p>
 * The VOLTRON management service provides two main functionality for the producers
 * (a.k.a managed entity) and consumer (a.k.a management entity) as follows:
 *     1. A message delivery infrastructure service that allows various types of
 *        messages to be pushed from manged entities to the management entity.
 *     2. A registry service that allows various types of management providers
 *        of the managed entity to be visible and available for consumption by
 *        the management entity.
 *
 * @author RKAV
 */
public interface ManagementService {
  /**
   * Get the message delivery infrastructure that consists of many producers
   * and possibly a single consumer.
   *
   * @return an interface to setup message delivery infrastructure
   */
  MessageDeliveryInfrastructureService getMessageDeliveryInfrastructure();


  /**
   * Registry service, which is an interface for managed entities to register themselves
   * and publish their managed objects and their capabilities. This service also lets
   * management entities to discover the managed entities and their capabilities.
   *
   * @return an interface for managed entities to register themselves, register their
   */
  RegistryService getRegistryService();
}
