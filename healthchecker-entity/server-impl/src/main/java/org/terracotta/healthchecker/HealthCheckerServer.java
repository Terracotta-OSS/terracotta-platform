/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ClientDescriptor;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.PassiveSynchronizationChannel;
import org.terracotta.healthchecker.HealthCheckReq;
import org.terracotta.healthchecker.HealthCheckRsp;

/**
 *
 * @author mscott
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
