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
package org.terracotta.management.service.monitoring;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.management.model.cluster.ClientIdentifier;

/**
 * Class used by active entities requiring to push some data into the monitoring service
 *
 * @author Mathieu Carbou
 */
@CommonComponent
public interface ActiveEntityMonitoringService extends EntityMonitoringService {

  /**
   * Returns an identifier in the management topology for an internal voltron client descriptor. This client identifier identifies a client cluster-wise
   * <p>
   * Can be called from active entity only
   */
  ClientIdentifier getClientIdentifier(ClientDescriptor clientDescriptor);

}
