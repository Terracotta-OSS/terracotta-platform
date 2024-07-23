/*
 * Copyright Terracotta, Inc.
 * Copyright Super iPaaS Integration LLC, an IBM Company 2024
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
package org.terracotta.healthchecker;

import org.terracotta.entity.ActiveInvokeContext;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.InvokeContext;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;

/**
 *
 */
public class HealthCheckerServer implements ActiveServerEntity<HealthCheckReq, HealthCheckRsp>, PassiveServerEntity<HealthCheckReq, HealthCheckRsp> {

  @Override
  public void connected(ClientDescriptor clientDescriptor) {

  }

  @Override
  public void disconnected(ClientDescriptor clientDescriptor) {

  }

  @Override
  public HealthCheckRsp invokeActive(ActiveInvokeContext<HealthCheckRsp> context, HealthCheckReq message) {
    return new HealthCheckRsp(message.toString());
  }

  public ActiveServerEntity.ReconnectHandler startReconnect() {
    return (ClientDescriptor clientDescriptor, byte[] extendedReconnectData)->{

    };
  }

  @Override
  public void synchronizeKeyToPassive(PassiveSynchronizationChannel<HealthCheckReq> syncChannel, int concurrencyKey) {

  }

  @Override
  public void createNew() {

  }

  @Override
  public void loadExisting() {

  }

  @Override
  public void destroy() {

  }  

  @Override
  public void invokePassive(InvokeContext context, HealthCheckReq message) {

  }

  @Override
  public void startSyncEntity() {

  }

  @Override
  public void endSyncEntity() {

  }

  @Override
  public void startSyncConcurrencyKey(int concurrencyKey) {

  }

  @Override
  public void endSyncConcurrencyKey(int concurrencyKey) {

  }
  
  
}
