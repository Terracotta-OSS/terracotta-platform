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
package org.terracotta.management.service;

import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.voltron.management.ManagementService;

/**
 * The configuration for the management service.
 * <p>
 * TODO: Currently this is just a place holder.
 *
 * @author RKAV
 */
public class ManagementServiceConfiguration implements ServiceConfiguration<ManagementService> {
  @Override
  public Class<ManagementService> getServiceType() {
    return ManagementService.class;
  }
}
