/*
 * All content copyright (c) 2014 Terracotta, Inc., except as may otherwise
 * be noted in a separate copyright notice. All rights reserved.
 */
package org.terracotta.healthchecker;

import java.util.Collections;
import java.util.Set;
import org.terracotta.entity.ActiveServerEntity;
import org.terracotta.entity.ConcurrencyStrategy;
import org.terracotta.entity.EntityMessage;
import org.terracotta.entity.MessageCodec;
import org.terracotta.entity.PassiveServerEntity;
import org.terracotta.entity.ServerEntityService;
import org.terracotta.entity.ServiceRegistry;
import org.terracotta.entity.SyncMessageCodec;

/**
 *
 * @author mscott
 */
public class HealthCheckServerEntityService implements ServerEntityService<HealthCheckReq, HealthCheckRsp> {
  
  private static final ConcurrencyStrategy CONCURRENCY = new ConcurrencyStrategy() {
    @Override
    public int concurrencyKey(EntityMessage message) {
      return ConcurrencyStrategy.UNIVERSAL_KEY;
    }

    @Override
    public Set getKeysForSynchronization() {
      return Collections.emptySet();
    }

  };

  @Override
  public long getVersion() {
    return 1L;
  }

  @Override
  public boolean handlesEntityType(String typeName) {
    return "org.terracotta.healthchecker.HealthCheck".equals(typeName);
  }

  @Override
  public ActiveServerEntity<HealthCheckReq, HealthCheckRsp> createActiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new HealthCheckerServer();
  }

  @Override
  public PassiveServerEntity<HealthCheckReq, HealthCheckRsp> createPassiveEntity(ServiceRegistry registry, byte[] configuration) {
    return new HealthCheckerServer();
  }

  @Override
  public ConcurrencyStrategy<HealthCheckReq> getConcurrencyStrategy(byte[] configuration) {
    return  CONCURRENCY;
  }

  @Override
  public MessageCodec<HealthCheckReq, HealthCheckRsp> getMessageCodec() {
    return new HealthCheckerCodec();
  }

  @Override
  public SyncMessageCodec<HealthCheckReq> getSyncMessageCodec() {
    return null;
  }
  
  
  
}
