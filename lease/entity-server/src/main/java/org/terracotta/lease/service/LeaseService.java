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
import org.terracotta.entity.ClientDescriptor;

/**
 * A service that the lease acquiring entity can use to manage the leases. Specifically it spins up a thread to
 * monitor the leases, which the entity should not do.
 */
@CommonComponent
public interface LeaseService {
  LeaseResult acquireLease(ClientDescriptor clientDescriptor);

  void disconnected(ClientDescriptor clientDescriptor);

  void reconnecting(ClientDescriptor clientDescriptor);

  void reconnected(ClientDescriptor clientDescriptor);
}
