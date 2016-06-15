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
 *  The Covered Software is Terracotta Core.
 *
 *  The Initial Developer of the Covered Software is
 *  Terracotta, Inc., a Software AG company
 *
 */
package org.terracotta.healthchecker;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
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
  public HealthCheckRsp invoke(ClientDescriptor clientDescriptor, HealthCheckReq message) {
    return new HealthCheckRsp(message.toString());
  }

  @Override
  public void handleReconnect(ClientDescriptor clientDescriptor, byte[] extendedReconnectData) {

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
  public void invoke(HealthCheckReq message) {

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
