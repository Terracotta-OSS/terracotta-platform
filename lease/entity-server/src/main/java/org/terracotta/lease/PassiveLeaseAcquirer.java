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
package org.terracotta.lease;

import org.terracotta.entity.ConfigurationException;
import org.terracotta.entity.EntityUserException;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;

/**
 * The passive server-side entity for connection leasing.
 */
class PassiveLeaseAcquirer implements PassiveServerEntity<LeaseMessage, LeaseResponse> {
  @Override
  public void invokePassive(InvokeContext context, LeaseMessage message) throws EntityUserException {
  }

  @Override
  public void startSyncEntity() {
  }

  @Override
  public void endSyncEntity() {
  }

  @Override
  public void startSyncConcurrencyKey(int i) {
  }

  @Override
  public void endSyncConcurrencyKey(int i) {
  }

  @Override
  public void createNew() throws ConfigurationException {
  }

  @Override
  public void destroy() {
  }
}
