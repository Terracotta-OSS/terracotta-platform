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
package org.terracotta.lease.service;

import com.tc.classloader.CommonComponent;
import org.terracotta.entity.ServiceConfiguration;
import org.terracotta.lease.service.closer.ClientConnectionCloser;

/**
 * The object that a LeaseService client can pass to the ServiceRegistry to get a LeaseService. Note that the
 * ClientConnectionCloser should be a ClientConnectionCloserImpl, i.e. it uses ClientCommunicator to close the
 * client connection. This object is only necessary because services cannot directly lookup their own dependencies.
 */
@CommonComponent
public class LeaseServiceConfiguration implements ServiceConfiguration<LeaseService> {
  private final ClientConnectionCloser clientConnectionCloser;

  public LeaseServiceConfiguration(ClientConnectionCloser clientConnectionCloser) {
    this.clientConnectionCloser = clientConnectionCloser;
  }

  @Override
  public Class<LeaseService> getServiceType() {
    return LeaseService.class;
  }

  public ClientConnectionCloser getClientConnectionCloser() {
    return clientConnectionCloser;
  }
}
