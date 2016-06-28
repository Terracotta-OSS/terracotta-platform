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
 *  The Covered Software is Entity Management Service.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.management.service.impl;

import org.terracotta.voltron.management.ManagementService;
import org.terracotta.voltron.management.MessageDeliveryInfrastructureService;
import org.terracotta.voltron.management.RegistryService;

/**
 * Default management service implementation.
 * <p>
 * TODO: Implement full functionality. Currently only the shell necessary for the PoC
 * (i.e message delivery) is implemented.
 *
 * @author RKAV
 */
public class DefaultManagementService implements ManagementService {
  private final MessageDeliveryInfrastructureService messagingInfrastructure;

  public DefaultManagementService() {
    messagingInfrastructure = new DefaultMessageDeliveryInfrastructure();
  }

  @Override
  public MessageDeliveryInfrastructureService getMessageDeliveryInfrastructure() {
    return messagingInfrastructure;
  }

  @Override
  public RegistryService getRegistryService() {
    throw new UnsupportedOperationException("Management Registry Not yet implemented");
  }
}
